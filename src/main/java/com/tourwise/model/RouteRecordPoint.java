package com.tourwise.model;

import lombok.Data;

@Data
public class RouteRecordPoint {
    private Long id;
    private Long routeRecordId;
    private Long poiId;
    private String pointName;
    private Integer sortOrder;
    private Integer distanceFromStartM;
    private String description;
}
