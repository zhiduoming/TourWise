package com.tourwise.dto;

import lombok.Data;

@Data
public class RouteGraphNodeRequest {
    private Long id;
    private String clientId;
    private String name;
    private String nodeType;
    private String categoryCode;
    private String areaCode;
    private String areaName;
    private Integer mapX;
    private Integer mapY;
    private Boolean visible;
}
