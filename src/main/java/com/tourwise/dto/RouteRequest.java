package com.tourwise.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {
    @NotBlank
    private String start;

    @NotBlank
    private String end;

    private List<String> waypoints;
    private List<String> preferences;
    private Long spotId;
    private Long placeGroupId;
}
