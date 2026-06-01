package com.tourwise.vo.route;

import lombok.Data;

import java.util.List;

@Data
public class RouteGraphDiffVO {
    private Long placeGroupId;
    private RouteGraphVersionVO version;
    private RouteGraphExportStatsVO currentStats;
    private RouteGraphExportStatsVO versionStats;
    private List<RouteGraphDiffNodeVO> addedNodes;
    private List<RouteGraphDiffNodeVO> removedNodes;
    private List<RouteGraphDiffNodeVO> movedNodes;
    private List<RouteGraphDiffEdgeVO> addedEdges;
    private List<RouteGraphDiffEdgeVO> removedEdges;
    private List<String> warnings;
}
