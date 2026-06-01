package com.tourwise.service.route;

import com.tourwise.model.RouteEdge;
import com.tourwise.model.RoutePoi;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Component
public class AStarRoutePlanner {
    private static final double MAP_PIXEL_TO_METER = 0.35;

    public RoutePathResult findPath(
            Long startId,
            Long endId,
            Map<Long, RoutePoi> pois,
            Map<Long, List<RouteEdge>> graph,
            RouteSearchOptions options) {
        RoutePoi end = pois.get(endId);
        if (!canEstimate(pois.get(startId), end)) {
            return null;
        }
        PriorityQueue<State> queue = new PriorityQueue<>(Comparator.comparingDouble(State::getPriority));
        Map<Long, Integer> best = new HashMap<>();
        Map<Long, DijkstraRoutePlanner.Previous> previous = new HashMap<>();
        queue.add(new State(startId, 0, heuristic(pois.get(startId), end)));
        best.put(startId, 0);

        while (!queue.isEmpty()) {
            State current = queue.poll();
            if (current.getCost() > best.getOrDefault(current.getNodeId(), Integer.MAX_VALUE)) {
                continue;
            }
            if (current.getNodeId().equals(endId)) {
                break;
            }
            for (RouteEdge edge : graph.getOrDefault(current.getNodeId(), List.of())) {
                RoutePoi next = pois.get(edge.getToPoiId());
                if (!canEstimate(next, end)) {
                    return null;
                }
                int nextCost = current.getCost() + RouteWeight.cost(edge, options);
                if (nextCost < best.getOrDefault(edge.getToPoiId(), Integer.MAX_VALUE)) {
                    best.put(edge.getToPoiId(), nextCost);
                    previous.put(edge.getToPoiId(), new DijkstraRoutePlanner.Previous(current.getNodeId(), edge));
                    queue.add(new State(edge.getToPoiId(), nextCost, nextCost + heuristic(next, end)));
                }
            }
        }

        if (!best.containsKey(endId)) {
            return null;
        }
        return DijkstraRoutePlanner.buildResult(startId, endId, previous, "astar");
    }

    private static boolean canEstimate(RoutePoi current, RoutePoi end) {
        return current != null && end != null
                && ((current.getMapX() != null && current.getMapY() != null
                && end.getMapX() != null && end.getMapY() != null)
                || (current.getLongitude() != null && current.getLatitude() != null
                && end.getLongitude() != null && end.getLatitude() != null));
    }

    private static double heuristic(RoutePoi current, RoutePoi end) {
        if (current.getMapX() != null && current.getMapY() != null
                && end.getMapX() != null && end.getMapY() != null) {
            double dx = current.getMapX() - end.getMapX();
            double dy = current.getMapY() - end.getMapY();
            return Math.sqrt(dx * dx + dy * dy) * MAP_PIXEL_TO_METER;
        }
        BigDecimal currentLng = current.getLongitude();
        BigDecimal currentLat = current.getLatitude();
        BigDecimal endLng = end.getLongitude();
        BigDecimal endLat = end.getLatitude();
        double dx = currentLng.subtract(endLng).doubleValue() * 85000;
        double dy = currentLat.subtract(endLat).doubleValue() * 111000;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Data
    @AllArgsConstructor
    private static class State {
        private Long nodeId;
        private int cost;
        private double priority;
    }
}
