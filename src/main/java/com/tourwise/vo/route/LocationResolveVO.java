package com.tourwise.vo.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationResolveVO {
    private CoordinateVO coordinate;
    private LocationMatchVO matchedSpot;
    private LocationMatchVO matchedPoi;
    private List<LocationMatchVO> candidates;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoordinateVO {
        private BigDecimal longitude;
        private BigDecimal latitude;
        private Integer accuracy;
        private String provider;
        private String coordinateSystem;
    }
}
