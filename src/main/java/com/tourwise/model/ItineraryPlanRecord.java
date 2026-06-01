package com.tourwise.model;

import lombok.Data;

@Data
public class ItineraryPlanRecord {
    private Long id;
    private Long userId;
    private Long sourcePlanId;
    private String title;
    private String city;
    private String duration;
    private String pace;
    private Integer totalDays;
    private Integer spotCount;
    private Integer copyCount;
    private Integer favoriteCount;
    private String summary;
    private String preferencesJson;
    private Integer status;
}
