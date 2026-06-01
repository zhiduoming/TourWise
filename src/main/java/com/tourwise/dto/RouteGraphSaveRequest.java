package com.tourwise.dto;

import com.tourwise.vo.route.RouteGraphExportStatsVO;
import com.tourwise.vo.route.RouteMapVO;
import lombok.Data;

import java.util.List;

@Data
public class RouteGraphSaveRequest {
    private String schemaVersion;
    private String exportedAt;
    private Long placeGroupId;
    private String placeGroupName;
    private String placeGroupShortName;
    private String routeGraphStatus;
    private String city;
    private String address;
    private RouteMapVO map;
    private RouteGraphExportStatsVO stats;
    private List<RouteGraphNodeRequest> nodes;
    private List<RouteGraphEdgeRequest> edges;
}
