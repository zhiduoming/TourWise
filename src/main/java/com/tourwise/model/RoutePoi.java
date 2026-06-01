package com.tourwise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoutePoi {
    private Long id;
    private String name;
    private String category;
    private String type;
    private String areaCode;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer mapX;
    private Integer mapY;
    private Long placeGroupId;
}
