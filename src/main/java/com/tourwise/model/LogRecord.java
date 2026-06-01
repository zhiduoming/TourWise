package com.tourwise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogRecord {
    private Long id;
    private Long userId;
    private Long poiId;
    private Long circleId;
    private Long itineraryPlanId;
    private String title;
    private String content;
    private BigDecimal rating;
    private BigDecimal sceneryRating;
    private BigDecimal facilityRating;
    private BigDecimal serviceRating;
    private BigDecimal trafficRating;
    private BigDecimal valueRating;
}
