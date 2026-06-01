package com.tourwise.service;

import com.tourwise.dto.*;
import com.tourwise.mapper.*;
import com.tourwise.model.*;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.tourwise.common.BusinessException;
import com.tourwise.config.OssProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OssStorageService {
    private final OssProperties properties;

    public OssStorageService(OssProperties properties) {
        this.properties = properties;
    }

    public String uploadAvatar(long userId, MultipartFile file) {
        return uploadImage(file, cleanPrefix(properties.getAvatarPrefix()), String.valueOf(userId), "头像");
    }

    public String uploadUserImage(long userId, MultipartFile file, String scene) {
        String normalizedScene = cleanScene(scene);
        String prefix = cleanPrefix(properties.getFilePrefix()) + "/" + normalizedScene;
        return uploadImage(file, prefix, String.valueOf(userId), "图片");
    }

    public String uploadRouteMap(long adminUserId, long placeGroupId, MultipartFile file) {
        String prefix = cleanPrefix(properties.getFilePrefix()) + "/maps/place-" + placeGroupId;
        return uploadImage(file, prefix, String.valueOf(adminUserId), "地图");
    }

    public String uploadAdminImage(long adminUserId, String targetKey, MultipartFile file) {
        String normalizedTarget = cleanScene(targetKey);
        String prefix = cleanPrefix(properties.getFilePrefix()) + "/admin-images/" + normalizedTarget;
        return uploadImage(file, prefix, String.valueOf(adminUserId), "图片");
    }

    private String uploadImage(MultipartFile file, String prefix, String owner, String label) {
        validateConfigured();
        validateImage(file, label);
        String extension = extensionOf(file.getOriginalFilename(), file.getContentType());
        String objectName = prefix + "/" + owner + "-" + UUID.randomUUID() + extension;

        OSS ossClient = new OSSClientBuilder().build(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret());
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            ossClient.putObject(properties.getBucket(), objectName, file.getInputStream(), metadata);
            return publicUrl(objectName);
        } catch (IOException ex) {
            throw BusinessException.badRequest(label + "文件读取失败");
        } catch (RuntimeException ex) {
            throw BusinessException.badRequest(label + "上传到 OSS 失败");
        } finally {
            ossClient.shutdown();
        }
    }

    private void validateConfigured() {
        if (!StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getBucket())
                || !StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getAccessKeySecret())
                || !StringUtils.hasText(properties.getPublicBaseUrl())) {
            throw BusinessException.badRequest("OSS 配置不完整，请设置 TOURWISE_OSS_ACCESS_KEY_ID 和 TOURWISE_OSS_ACCESS_KEY_SECRET");
        }
    }

    private static void validateImage(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择" + label + "文件");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw BusinessException.badRequest(label + "大小不能超过 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw BusinessException.badRequest(label + "必须是图片文件");
        }
    }

    private String publicUrl(String objectName) {
        String baseUrl = properties.getPublicBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/" + objectName;
    }

    private static String cleanPrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "uploads";
        }
        return prefix.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private static String cleanScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            return "common";
        }
        String normalized = scene.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "");
        return StringUtils.hasText(normalized) ? normalized : "common";
    }

    private static String extensionOf(String originalName, String contentType) {
        if (StringUtils.hasText(originalName) && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
            if (ext.length() <= 10) {
                return ext;
            }
        }
        Map<String, String> byType = new LinkedHashMap<>();
        byType.put("image/jpeg", ".jpg");
        byType.put("image/png", ".png");
        byType.put("image/gif", ".gif");
        byType.put("image/webp", ".webp");
        return byType.getOrDefault(contentType, ".png");
    }
}
