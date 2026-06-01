package com.tourwise.vo.route;

import lombok.Data;

import java.util.List;

@Data
public class RouteGraphVO {
    private Long placeGroupId;
    private RouteMapVO map;
    private List<RouteGraphNodeVO> nodes;
    private List<RouteGraphEdgeVO> edges;
}
