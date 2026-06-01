package com.tourwise.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminPoiRequest {
    private Long id;
    private Long categoryId;
    private String name;
    private String scene;
    private String areaCode;
    private String areaName;
    private String province;
    private String city;
    private String address;
    private String locationText;
    private String description;
    private String imageUrl;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer mapX;
    private Integer mapY;
    private BigDecimal rating;
    private Integer hotness;
    private Integer status;
}
