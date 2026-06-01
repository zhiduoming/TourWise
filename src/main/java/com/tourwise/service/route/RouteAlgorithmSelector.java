package com.tourwise.service.route;

import com.tourwise.model.RouteEdge;
import com.tourwise.model.RoutePoi;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RouteAlgorithmSelector {
    private final AStarRoutePlanner aStarRoutePlanner;
    private final DijkstraRoutePlanner dijkstraRoutePlanner;

    public RouteAlgorithmSelector(AStarRoutePlanner aStarRoutePlanner, DijkstraRoutePlanner dijkstraRoutePlanner) {
        this.aStarRoutePlanner = aStarRoutePlanner;
        this.dijkstraRoutePlanner = dijkstraRoutePlanner;
    }

    public RoutePathResult findPath(
            RoutePoi start,
            RoutePoi end,
            Map<Long, RoutePoi> pois,
            Map<Long, List<RouteEdge>> graph,
            RouteSearchOptions options) {
        RoutePathResult astar = aStarRoutePlanner.findPath(start.getId(), end.getId(), pois, graph, options);
        if (astar != null) {
            return astar;
        }
        return dijkstraRoutePlanner.findPath(start.getId(), end.getId(), graph, options);
    }
}
