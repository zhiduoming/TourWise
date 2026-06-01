package com.tourwise.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentReportRecord {
    private Long id;
    private String targetType;
    private Long targetId;
    private Long reporterId;
    private String reason;
    private String detail;
    private String status;
    private Long handlerId;
    private String handleNote;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
