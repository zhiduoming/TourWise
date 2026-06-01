package com.tourwise.model;

import lombok.Data;

@Data
public class NotificationRecord {
    private Long id;
    private Long userId;
    private Long actorUserId;
    private String type;
    private String title;
    private String content;
    private String targetType;
    private Long targetId;
    private String linkUrl;
    private Integer isRead;
}
