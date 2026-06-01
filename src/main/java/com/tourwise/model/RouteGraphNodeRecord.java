package com.tourwise.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RouteGraphNodeRecord {
    private Long id;
    private Long spotId;
    private Long placeGroupId;
    private String name;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String areaCode;
    private String areaName;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer mapX;
    private Integer mapY;
}
