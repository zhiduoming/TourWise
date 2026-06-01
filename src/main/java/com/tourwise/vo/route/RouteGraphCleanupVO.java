package com.tourwise.vo.route;

import lombok.Data;

@Data
public class RouteGraphCleanupVO {
    private Long placeGroupId;
    private Integer invalidEdgeCount;
    private Integer crossGroupEdgeCount;
    private Integer selfLoopEdgeCount;
    private Integer duplicateEdgeCount;
    private Integer totalCount;
    private String message;
    private RouteGraphInspectionVO quality;
}
