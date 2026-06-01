package com.tourwise.model;

import lombok.Data;

@Data
public class RouteRecord {
    private Long id;
    private Long userId;
    private String routeName;
    private String mode;
    private Integer totalDistanceM;
    private Integer totalDurationMin;
    private String preferences;
}
