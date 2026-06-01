package com.tourwise.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RouteGraphVersionRecord {
    private Long id;
    private Long placeGroupId;
    private Integer versionNo;
    private String name;
    private String snapshotJson;
    private Integer nodeCount;
    private Integer edgeCount;
    private Long createdBy;
    private String createdByName;
    private String remark;
    private LocalDateTime createdAt;
}
