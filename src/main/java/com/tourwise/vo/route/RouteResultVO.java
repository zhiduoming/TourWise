package com.tourwise.vo.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteResultVO {
    private String provider;
    private String algorithm;
    private Integer duration;
    private Integer distance;
    private List<RoutePointVO> points;
    private List<RoutePolylinePointVO> polyline;
    private List<RouteStepVO> steps;
    private String message;

    public RouteResultVO(Integer duration, Integer distance, List<RoutePointVO> points, String message) {
        this.duration = duration;
        this.distance = distance;
        this.points = points;
        this.message = message;
    }
}
