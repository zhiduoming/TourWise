package com.tourwise.dto;

import lombok.Data;

import java.util.List;

@Data
public class RouteRecordSaveRequest {
    private String routeName;
    private String routeType;
    private String provider;
    private String algorithm;
    private String mode;
    private Long placeGroupId;
    private Long spotId;
    private Long sourcePlanId;
    private String sourcePlanTitle;
    private String startName;
    private String endName;
    private Integer totalDistanceM;
    private Integer totalDurationMin;
    private List<String> preferences;
    private List<RouteRecordPointRequest> points;
}
