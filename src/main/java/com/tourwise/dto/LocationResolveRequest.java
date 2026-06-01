package com.tourwise.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LocationResolveRequest {
    private String mode;
    private Long placeGroupId;

    @NotNull
    private BigDecimal longitude;

    @NotNull
    private BigDecimal latitude;

    private Integer accuracy;
    private String provider;
    private String coordinateSystem;
}
