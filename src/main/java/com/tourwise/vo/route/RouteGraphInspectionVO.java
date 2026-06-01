package com.tourwise.vo.route;

import lombok.Data;

import java.util.List;

@Data
public class RouteGraphInspectionVO {
    private Long placeGroupId;
    private Integer poiCount;
    private Integer routeNodeCount;
    private Integer edgeCount;
    private Integer issueCount;
    private Integer blockingIssueCount;
    private List<RouteGraphIssueVO> issues;
}
