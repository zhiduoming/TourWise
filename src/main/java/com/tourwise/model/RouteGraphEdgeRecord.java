package com.tourwise.model;

import lombok.Data;

@Data
public class RouteGraphEdgeRecord {
    private Long id;
    private Long fromPoiId;
    private Long toPoiId;
    private Integer distanceM;
    private Integer durationMin;
    private String transportType;
    private Boolean indoor;
    private String description;
}
