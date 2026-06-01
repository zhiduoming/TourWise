package com.tourwise.vo.admin;

import lombok.Data;

import java.util.List;

@Data
public class RouteGraphQualityVO {
    private Long placeGroupId;
    private String name;
    private String routeGraphStatus;
    private Boolean hasMap;
    private Integer mapWidth;
    private Integer mapHeight;
    private Integer visiblePoiCount;
    private Integer routeNodeCount;
    private Integer edgeCount;
    private Integer missingGeoCount;
    private Integer missingMapPointCount;
    private Integer isolatedPoiCount;
    private Integer isolatedRouteNodeCount;
    private Integer unreachablePoiCount;
    private Integer suspiciousEdgeCount;
    private String level;
    private List<String> issues;
}
