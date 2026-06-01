package com.tourwise.dto;

import lombok.Data;

@Data
public class RouteRecordPointRequest {
    private Long poiId;
    private String pointName;
    private Integer distanceFromStartM;
    private String description;
}
