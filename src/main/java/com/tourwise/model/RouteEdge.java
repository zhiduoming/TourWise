package com.tourwise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteEdge {
    private Long id;
    private Long fromPoiId;
    private Long toPoiId;
    private Integer distanceM;
    private Integer durationMin;
    private BigDecimal congestionFactor;
    private Boolean indoor;
    private String transportType;
    private String description;
}
