package com.tourwise.security;

import com.tourwise.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtService {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final Pattern SUB_PATTERN = Pattern.compile("\"sub\"\\s*:\\s*(\\d+)");
    private static final Pattern EXP_PATTERN = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

    @Value("${tourwise.jwt.secret}")
    private String secret;

    @Value("${tourwise.jwt.expire-hours:168}")
    private long expireHours;

    public String createToken(long userId, String username) {
        try {
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String safeUsername = username == null ? "" : username.replace("\\", "\\\\").replace("\"", "\\\"");
            long exp = Instant.now().plusSeconds(expireHours * 3600).getEpochSecond();
            String payload = "{\"sub\":" + userId + ",\"username\":\"" + safeUsername + "\",\"exp\":" + exp + "}";
            String headerPart = encode(header.getBytes(StandardCharsets.UTF_8));
            String payloadPart = encode(payload.getBytes(StandardCharsets.UTF_8));
            String signingInput = headerPart + "." + payloadPart;
            return signingInput + "." + encode(sign(signingInput));
        } catch (Exception ex) {
            throw BusinessException.badRequest("token 生成失败");
        }
    }

    public Long parseUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String signingInput = parts[0] + "." + parts[1];
            String expected = encode(sign(signingInput));
            if (!constantTimeEquals(expected, parts[2])) {
                return null;
            }
            String payload = new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
            Long exp = findLong(EXP_PATTERN, payload);
            if (exp == null || exp < Instant.now().getEpochSecond()) {
                return null;
            }
            return findLong(SUB_PATTERN, payload);
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] sign(String input) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(byte[] bytes) {
        return URL_ENCODER.encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static Long findLong(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }
}
