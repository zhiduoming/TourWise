package com.tourwise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.config.ZhipuProperties;
import com.tourwise.mapper.LogMapper;
import com.tourwise.security.AuthContext;
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

/**
 * 调用智谱 CogVideoX 图生视频 API，按"提交任务→轮询状态→回写日志"的异步模式工作。
 * 课设要求 (4)-⑨：AIGC 旅游动画。
 */
@Service
public class VideoGenerationService {
    private static final Logger log = LoggerFactory.getLogger(VideoGenerationService.class);

    private final ZhipuProperties zhipuProperties;
    private final LogMapper logMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public VideoGenerationService(ZhipuProperties zhipuProperties, LogMapper logMapper) {
        this.zhipuProperties = zhipuProperties;
        this.logMapper = logMapper;
    }

    /**
     * 提交一个图生视频任务：取日记第一张照片作为首帧、标题/内容拼成提示词，调用智谱接口。
     * 返回任务 ID 后立即结束，前端按 queryStatus 轮询结果。
     */
    public Map<String, Object> submit(Long logId, boolean force) {
        ensureConfigured();
        Long userId = AuthContext.requireUserId();

        Map<String, Object> info = requireAnimationInfo(logId);
        Long ownerId = toLong(info.get("userId"));
        if (ownerId == null || !ownerId.equals(userId)) {
            throw BusinessException.unauthorized("只能为自己的日记生成动画");
        }

        String status = stringOf(info.get("animationStatus"));
        if (!force && "success".equals(status)) {
            throw BusinessException.badRequest("该日记已经生成过动画，如需重新生成请使用强制重试");
        }
        if (!force && "processing".equals(status)) {
            throw BusinessException.badRequest("动画生成中，请稍后查询");
        }

        String image = stringOf(info.get("firstImage"));
        if (!StringUtils.hasText(image)) {
            throw BusinessException.badRequest("请先在日记中上传至少一张照片");
        }
        boolean isFood = info.get("foodId") != null;
        String prompt = isFood
                ? buildFoodPrompt(
                        stringOf(info.get("title")),
                        stringOf(info.get("content")),
                        stringOf(info.get("foodName")),
                        stringOf(info.get("foodCuisine")))
                : buildSpotPrompt(
                        stringOf(info.get("title")),
                        stringOf(info.get("content")),
                        stringOf(info.get("poiName")));

        String taskId = callSubmit(image, prompt);
        logMapper.updateAnimationSubmitted(logId, taskId);
        return Map.of("task_id", taskId, "status", "processing");
    }

    /**
     * 查询任务状态，必要时回写视频 URL。返回当前状态供前端轮询。
     */
    public Map<String, Object> query(Long logId) {
        Map<String, Object> info = requireAnimationInfo(logId);
        String status = stringOf(info.get("animationStatus"));
        String taskId = stringOf(info.get("animationTaskId"));

        if (!"processing".equals(status) || !StringUtils.hasText(taskId)) {
            return buildStatusResponse(info);
        }

        try {
            JsonNode result = callQuery(taskId);
            String taskStatus = textOrEmpty(result, "task_status").toUpperCase();
            if ("SUCCESS".equals(taskStatus)) {
                JsonNode videos = result.path("video_result");
                if (videos.isArray() && videos.size() > 0) {
                    String videoUrl = textOrEmpty(videos.get(0), "url");
                    String coverUrl = textOrEmpty(videos.get(0), "cover_image_url");
                    if (StringUtils.hasText(videoUrl)) {
                        logMapper.updateAnimationSuccess(logId, videoUrl, coverUrl);
                        return Map.of(
                                "status", "success",
                                "animation_url", videoUrl,
                                "animation_cover_url", coverUrl == null ? "" : coverUrl
                        );
                    }
                }
                logMapper.updateAnimationFailed(logId, "智谱返回成功但无视频地址");
                return Map.of("status", "failed", "animation_error", "智谱返回成功但无视频地址");
            }
            if ("FAIL".equals(taskStatus) || "FAILED".equals(taskStatus)) {
                String reason = textOrEmpty(result, "task_status_reason");
                if (!StringUtils.hasText(reason)) {
                    reason = "智谱任务失败";
                }
                logMapper.updateAnimationFailed(logId, truncate(reason, 480));
                return Map.of("status", "failed", "animation_error", reason);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询智谱视频任务失败 logId={}, taskId={}", logId, taskId, e);
        }
        return Map.of("status", "processing", "task_id", taskId);
    }

    private String callSubmit(String imageUrl, String prompt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", zhipuProperties.getVideoModel());
            body.put("image_url", imageUrl);
            if (StringUtils.hasText(prompt)) {
                body.put("prompt", prompt);
            }
            body.put("with_audio", false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(zhipuProperties.getBaseUrl() + "/videos/generations"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + zhipuProperties.getApiKey())
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("智谱视频生成提交失败 status={} body={}", response.statusCode(), response.body());
                throw BusinessException.badRequest("智谱视频服务暂时不可用：" + response.statusCode());
            }
            JsonNode json = objectMapper.readTree(response.body());
            String taskId = textOrEmpty(json, "id");
            if (!StringUtils.hasText(taskId)) {
                throw new RuntimeException("智谱未返回任务 ID：" + response.body());
            }
            return taskId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用智谱视频生成接口失败", e);
            throw BusinessException.badRequest("AI 视频服务调用失败：" + e.getMessage());
        }
    }

    private JsonNode callQuery(String taskId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(zhipuProperties.getBaseUrl() + "/async-result/" + taskId))
                .header("Authorization", "Bearer " + zhipuProperties.getApiKey())
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            log.warn("智谱视频任务查询返回非 2xx status={} body={}", response.statusCode(), response.body());
            throw new RuntimeException("查询失败：" + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private Map<String, Object> requireAnimationInfo(Long logId) {
        Map<String, Object> raw = logMapper.findAnimationInfo(logId);
        if (raw == null) {
            throw BusinessException.notFound("日记不存在");
        }
        return MapUtil.normalize(raw);
    }

    private Map<String, Object> buildStatusResponse(Map<String, Object> info) {
        String status = stringOf(info.get("animationStatus"));
        String url = stringOf(info.get("animationUrl"));
        String cover = stringOf(info.get("animationCoverUrl"));
        String error = stringOf(info.get("animationError"));
        String taskId = stringOf(info.get("animationTaskId"));
        return Map.of(
                "status", status == null ? "idle" : status,
                "animation_url", url == null ? "" : url,
                "animation_cover_url", cover == null ? "" : cover,
                "animation_error", error == null ? "" : error,
                "task_id", taskId == null ? "" : taskId
        );
    }

    private String buildSpotPrompt(String title, String content, String poiName) {
        StringBuilder sb = new StringBuilder();
        sb.append("电影感旅行 vlog 镜头：以这张照片为起点，镜头缓慢向前推进并轻微环绕，");
        sb.append("画面中的景物保持透视一致，注意细节自然运动——");
        sb.append("树叶随风轻摆、云层缓缓流动、水面波光粼粼、行人和车辆缓慢移动、阳光投下柔和的丁达尔光束；");
        sb.append("色调温暖明亮，景深略浅，仿佛旅行者举着手机记录第一视角，结尾镜头微微抬升停在画面正中。");
        if (StringUtils.hasText(poiName)) {
            sb.append("地点：").append(poiName.trim()).append("。");
        }
        if (StringUtils.hasText(title)) {
            sb.append("场景主题：").append(title.trim()).append("。");
        }
        if (StringUtils.hasText(content)) {
            String trimmed = content.trim();
            sb.append("旅行者的感受：").append(trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed).append("。");
        }
        sb.append("禁止变形扭曲，禁止出现文字水印。");
        return sb.toString();
    }

    private String buildFoodPrompt(String title, String content, String foodName, String cuisine) {
        StringBuilder sb = new StringBuilder();
        sb.append("美食探店 vlog 特写镜头：以这张菜品照片为起点，镜头缓慢拉近，再轻微环绕拍摄食物细节，");
        sb.append("呈现真实可口的就餐瞬间——食物表面蒸腾起细腻的热气、汤汁微微沸腾、油光在灯光下闪烁、");
        sb.append("筷子或勺子缓缓夹起一份食材并轻轻提起、汤汁顺势滴落、蘸料或浇头自然流动；");
        sb.append("画面温馨明亮，浅景深突出食物质感，背景虚化呈现餐桌氛围；");
        sb.append("结尾镜头微微抬升停在食物正上方，仿佛食客忍不住举起手机记录的第一视角。");
        if (StringUtils.hasText(foodName)) {
            sb.append("店家与菜品：").append(foodName.trim());
            if (StringUtils.hasText(cuisine)) {
                sb.append("（").append(cuisine.trim()).append("）");
            }
            sb.append("。");
        }
        if (StringUtils.hasText(title)) {
            sb.append("探店主题：").append(title.trim()).append("。");
        }
        if (StringUtils.hasText(content)) {
            String trimmed = content.trim();
            sb.append("食客评价：").append(trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed).append("。");
        }
        sb.append("禁止出现文字水印，禁止食物变形扭曲，禁止出现人脸正面。");
        return sb.toString();
    }

    private void ensureConfigured() {
        if (!zhipuProperties.isConfigured()) {
            throw BusinessException.badRequest("AI 视频功能暂未配置，请联系管理员");
        }
    }

    private static String textOrEmpty(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return "";
        }
        return node.path(field).asText("");
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String stringOf(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isEmpty() ? null : text;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
