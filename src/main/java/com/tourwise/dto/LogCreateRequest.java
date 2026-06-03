package com.tourwise.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogCreateRequest {
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 10000)
    private String content;

    private Long spotId;
    private Long poiId;
    private Long foodId;
    private Long circleId;
    private Long itineraryPlanId;

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private BigDecimal rating;

    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private BigDecimal sceneryRating;

    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private BigDecimal facilityRating;

    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private BigDecimal serviceRating;

    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private BigDecimal trafficRating;

    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private BigDecimal valueRating;

    private List<String> images;
    private List<String> tags;
}
