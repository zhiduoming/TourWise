package com.tourwise.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "tourwise.amap")
public class AmapProperties {
    private static final String ENV_ENABLED = "TOURWISE_AMAP_ENABLED";
    private static final String ENV_JS_KEY = "TOURWISE_AMAP_JS_KEY";
    private static final String ENV_SECURITY_CODE = "TOURWISE_AMAP_SECURITY_CODE";
    private static final String ENV_WEB_KEY = "TOURWISE_AMAP_WEB_KEY";

    private boolean enabled;
    private String jsKey;
    private String securityCode;
    private String webKey;

    @PostConstruct
    public void fillFromLocalShellWhenNecessary() {
        if (configured()) {
            return;
        }
        Map<String, String> localEnv = LocalShellEnv.loadZshrc(
                ENV_ENABLED,
                ENV_JS_KEY,
                ENV_SECURITY_CODE,
                ENV_WEB_KEY);
        if (hasText(localEnv.get(ENV_ENABLED))) {
            enabled = Boolean.parseBoolean(localEnv.get(ENV_ENABLED));
        }
        jsKey = firstText(jsKey, localEnv.get(ENV_JS_KEY));
        securityCode = firstText(securityCode, localEnv.get(ENV_SECURITY_CODE));
        webKey = firstText(webKey, localEnv.get(ENV_WEB_KEY));
    }

    public boolean configured() {
        return enabled
                && hasText(jsKey)
                && hasText(securityCode)
                && hasText(webKey);
    }

    private static String firstText(String primary, String fallback) {
        return hasText(primary) ? primary : fallback;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
