package com.tourwise.vo.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationMatchVO {
    private Long id;
    private Long spotId;
    private Long poiId;
    private Long placeGroupId;
    private String name;
    private String type;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer distance;
    private String confidence;
}
