package com.tourwise.service.route;

import com.tourwise.model.RouteEdge;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Component
public class DijkstraRoutePlanner {
    public RoutePathResult findPath(
            Long startId,
            Long endId,
            Map<Long, List<RouteEdge>> graph,
            RouteSearchOptions options) {
        PriorityQueue<State> queue = new PriorityQueue<>(Comparator.comparingInt(State::getCost));
        Map<Long, Integer> best = new HashMap<>();
        Map<Long, Previous> previous = new HashMap<>();
        queue.add(new State(startId, 0));
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
                int nextCost = current.getCost() + RouteWeight.cost(edge, options);
                if (nextCost < best.getOrDefault(edge.getToPoiId(), Integer.MAX_VALUE)) {
                    best.put(edge.getToPoiId(), nextCost);
                    previous.put(edge.getToPoiId(), new Previous(current.getNodeId(), edge));
                    queue.add(new State(edge.getToPoiId(), nextCost));
                }
            }
        }

        if (!best.containsKey(endId)) {
            return null;
        }
        return buildResult(startId, endId, previous, "dijkstra");
    }

    static RoutePathResult buildResult(
            Long startId,
            Long endId,
            Map<Long, Previous> previous,
            String algorithm) {
        List<RouteEdge> edges = new ArrayList<>();
        Long cursor = endId;
        while (!cursor.equals(startId)) {
            Previous prev = previous.get(cursor);
            if (prev == null) {
                return null;
            }
            edges.add(0, prev.getEdge());
            cursor = prev.getNodeId();
        }
        int distance = edges.stream().mapToInt(RouteEdge::getDistanceM).sum();
        int duration = edges.stream().mapToInt(RouteEdge::getDurationMin).sum();
        return new RoutePathResult(startId, endId, edges, distance, duration, algorithm);
    }

    @Data
    @AllArgsConstructor
    private static class State {
        private Long nodeId;
        private int cost;
    }

    @Data
    @AllArgsConstructor
    static class Previous {
        private Long nodeId;
        private RouteEdge edge;
    }
}
