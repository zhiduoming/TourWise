package com.tourwise.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tourwise.oss")
public class OssProperties {
    private String endpoint;
    private String bucket;
    private String accessKeyId;
    private String accessKeySecret;
    private String publicBaseUrl;
    private String avatarPrefix;
    private String filePrefix;
}
