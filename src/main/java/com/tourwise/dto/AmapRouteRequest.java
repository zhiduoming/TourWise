package com.tourwise.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmapRouteRequest {
    @Valid
    @NotNull
    private GeoPoint origin;

    @Valid
    @NotNull
    private GeoPoint destination;

    private List<GeoPoint> waypoints;
    private String mode;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoPoint {
        @NotNull
        private BigDecimal longitude;

        @NotNull
        private BigDecimal latitude;

        private String name;
    }
}
