package com.tourwise.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptimalRouteRequest {
    @NotEmpty
    private List<String> points;

    private List<String> preferences;
    private Long spotId;
    private Long placeGroupId;
}
