package com.tourwise.service.route;

import com.tourwise.model.RouteEdge;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoutePathResult {
    private Long startId;
    private Long endId;
    private List<RouteEdge> edges;
    private int distance;
    private int duration;
    private String algorithm;
}
