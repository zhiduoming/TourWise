package com.tourwise.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminSpotUpdateRequest {
    private Long categoryId;
    private String name;
    private String shortName;
    private String spotType;
    private String province;
    private String city;
    private String district;
    private String address;
    private String description;
    private String coverImage;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer locationRadiusM;
    private BigDecimal rating;
    private Integer hotness;
    private Integer status;
    private String routeGraphStatus;
}
