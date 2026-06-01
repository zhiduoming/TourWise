package com.tourwise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.mapper.SearchMapper;
import com.tourwise.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
public class AiSummaryService {
    private static final Logger log = LoggerFactory.getLogger(AiSummaryService.class);

    private final AiProperties aiProperties;
    private final SearchMapper searchMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AiSummaryService(AiProperties aiProperties, SearchMapper searchMapper) {
        this.aiProperties = aiProperties;
        this.searchMapper = searchMapper;
    }

    public String getOrGenerate(Long poiId, boolean force) {
        if (!aiProperties.isConfigured()) {
            throw BusinessException.badRequest("AI 功能暂未配置，请联系管理员");
        }

        if (!force) {
            String cached = searchMapper.findAiSummary(poiId);
            if (StringUtils.hasText(cached)) {
                return cached;
            }
        }

        Map<String, Object> raw = searchMapper.findFacilityById(poiId);
        if (raw == null) {
            throw BusinessException.notFound("景点不存在");
        }
        Map<String, Object> spot = MapUtil.normalize(raw);

        String summary = callDeepSeek(buildPrompt(spot));
        searchMapper.saveAiSummary(poiId, summary);
        return summary;
    }

    private String buildPrompt(Map<String, Object> spot) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的旅游导览助手。请根据以下景点信息，用250到300字生成一段有感染力的中文景点简介，");
        sb.append("内容包括：景点特色与亮点、游玩体验与氛围、适合人群或推荐理由。");
        sb.append("语气自然亲切，像导游讲解一样娓娓道来，不要逐条列出字段，不要出现综合评分、热度等数字指标。\n\n");

        appendIfPresent(sb, "景点名称", spot, "name");
        appendIfPresent(sb, "所属园区", spot, "placeGroupName");
        appendIfPresent(sb, "类型", spot, "categoryName");
        appendIfPresent(sb, "地区", spot, "district");
        appendIfPresent(sb, "地址", spot, "address");

        String openTime = str(spot, "openTime");
        String closeTime = str(spot, "closeTime");
        if (StringUtils.hasText(openTime) && StringUtils.hasText(closeTime)) {
            sb.append("开放时间：").append(openTime).append(" – ").append(closeTime).append("\n");
        }

        Object rating = spot.get("rating");
        if (rating != null) {
            sb.append("综合评分：").append(rating).append("/5.0\n");
        }
        appendIfPresent(sb, "现有描述", spot, "description");

        return sb.toString();
    }

    private String callDeepSeek(String prompt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", aiProperties.getModel());
            body.put("max_tokens", 600);
            body.put("temperature", 0.7);

            ArrayNode messages = body.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiProperties.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("DeepSeek API error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("AI 服务请求失败，状态码：" + response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String content = json.path("choices").path(0).path("message").path("content").asText();
            if (!StringUtils.hasText(content)) {
                throw new RuntimeException("AI 返回内容为空");
            }
            return content.trim();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 DeepSeek 失败", e);
            throw new RuntimeException("AI 服务暂时不可用：" + e.getMessage());
        }
    }

    private void appendIfPresent(StringBuilder sb, String label, Map<String, Object> map, String key) {
        String val = str(map, key);
        if (StringUtils.hasText(val)) {
            sb.append(label).append("：").append(val).append("\n");
        }
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : v.toString();
    }
}
