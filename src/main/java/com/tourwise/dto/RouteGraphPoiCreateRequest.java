package com.tourwise.dto;

import lombok.Data;

@Data
public class RouteGraphPoiCreateRequest {
    private String name;
    private String categoryCode;
    private String areaCode;
    private String areaName;
    private Integer mapX;
    private Integer mapY;
}
