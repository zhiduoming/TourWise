package com.tourwise.dto;

import lombok.Data;

import java.util.List;

@Data
public class ItineraryPlanSaveRequest {
    private String title;
    private String city;
    private String duration;
    private String pace;
    private Integer totalDays;
    private Integer spotCount;
    private String summary;
    private List<String> preferences;
    private List<ItineraryDaySaveRequest> days;
}
