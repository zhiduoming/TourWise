package com.tourwise.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItineraryPlanItemRecord {
    private Long id;
    private Long planId;
    private Integer dayNo;
    private Integer orderNo;
    private String itemType;
    private String timeSlot;
    private Long targetId;
    private Long spotId;
    private Long placeGroupId;
    private String name;
    private String description;
    private String address;
    private String imageUrl;
    private BigDecimal rating;
    private Integer hotness;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String recommendReason;
}
