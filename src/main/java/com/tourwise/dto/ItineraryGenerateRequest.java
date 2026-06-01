package com.tourwise.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ItineraryGenerateRequest {
    private String city;

    /**
     * half_day / one_day / two_day / three_day
     */
    private String duration;

    /**
     * relaxed / normal / compact
     */
    private String pace;

    /**
     * balanced / photo / study / food / relax / route
     */
    private String purpose;

    @Size(max = 8, message = "偏好标签最多选择8个")
    private List<String> preferences;

    private Boolean includeFood;

    private Boolean routeRequired;

    private Boolean avoidVisited;

    /**
     * economy / normal / premium
     */
    private String budgetLevel;
}
