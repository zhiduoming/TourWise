package com.tourwise.vo.itinerary;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryPlanVO {
    private Long id;
    private String title;
    private String city;
    private String duration;
    private String pace;
    private Integer totalDays;
    private Integer spotCount;
    private Integer copyCount;
    private Integer favoriteCount;
    private Boolean favorited;
    private Long sourcePlanId;
    private String sourcePlanTitle;
    private String ownerName;
    private String summary;
    private String routeHint;
    private String createdAt;
    private String updatedAt;
    private List<String> preferences = new ArrayList<>();
    private List<ItineraryDayVO> days = new ArrayList<>();
    private List<String> tips = new ArrayList<>();
}
