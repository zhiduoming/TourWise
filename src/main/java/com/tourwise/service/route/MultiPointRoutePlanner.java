package com.tourwise.service.route;

import com.tourwise.model.RouteEdge;
import com.tourwise.model.RoutePoi;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class MultiPointRoutePlanner {
    private final RouteAlgorithmSelector algorithmSelector;

    public MultiPointRoutePlanner(RouteAlgorithmSelector algorithmSelector) {
        this.algorithmSelector = algorithmSelector;
    }

    public OrderedRoute optimize(
            List<RoutePoi> routeNodes,
            Map<Long, RoutePoi> pois,
            Map<Long, List<RouteEdge>> graph,
            RouteSearchOptions options) {
        if (routeNodes.size() <= 3) {
            return new OrderedRoute(routeNodes, "astar");
        }
        List<RoutePoi> ordered = routeNodes.size() <= 10
                ? optimizeByDynamicProgramming(routeNodes, pois, graph, options)
                : optimizeByNearestNeighbor(routeNodes, pois, graph, options);
        return new OrderedRoute(ordered, routeNodes.size() <= 10 ? "tsp_dp" : "nearest_2opt");
    }

    private List<RoutePoi> optimizeByDynamicProgramming(
            List<RoutePoi> routeNodes,
            Map<Long, RoutePoi> pois,
            Map<Long, List<RouteEdge>> graph,
            RouteSearchOptions options) {
        int n = routeNodes.size();
        int middleCount = n - 2;
        RoutePathResult[][] paths = pairPaths(routeNodes, pois, graph, options);
        int stateCount = 1 << middleCount;
        int[][] dp = new int[stateCount][middleCount];
        int[][] prev = new int[stateCount][middleCount];
        for (int mask = 0; mask < stateCount; mask++) {
            java.util.Arrays.fill(dp[mask], Integer.MAX_VALUE / 4);
            java.util.Arrays.fill(prev[mask], -1);
        }
        for (int i = 0; i < middleCount; i++) {
            RoutePathResult path = paths[0][i + 1];
            if (path != null) {
                dp[1 << i][i] = path.getDistance();
            }
        }
        for (int mask = 0; mask < stateCount; mask++) {
            for (int last = 0; last < middleCount; last++) {
                if (dp[mask][last] >= Integer.MAX_VALUE / 4) {
                    continue;
                }
                for (int next = 0; next < middleCount; next++) {
                    if ((mask & (1 << next)) != 0) {
                        continue;
                    }
                    RoutePathResult path = paths[last + 1][next + 1];
                    if (path == null) {
                        continue;
                    }
                    int nextMask = mask | (1 << next);
                    int candidate = dp[mask][last] + path.getDistance();
                    if (candidate < dp[nextMask][next]) {
                        dp[nextMask][next] = candidate;
                        prev[nextMask][next] = last;
                    }
                }
            }
        }
        int full = stateCount - 1;
        int bestLast = -1;
        int bestCost = Integer.MAX_VALUE;
        for (int last = 0; last < middleCount; last++) {
            RoutePathResult toEnd = paths[last + 1][n - 1];
            if (toEnd == null) {
                continue;
            }
            int candidate = dp[full][last] + toEnd.getDistance();
            if (candidate < bestCost) {
                bestCost = candidate;
                bestLast = last;
            }
        }
        if (bestLast < 0) {
            return routeNodes;
        }
        List<RoutePoi> order = new ArrayList<>();
        int mask = full;
        int cursor = bestLast;
        while (cursor >= 0) {
            order.add(routeNodes.get(cursor + 1));
            int prevCursor = prev[mask][cursor];
            mask ^= 1 << cursor;
            cursor = prevCursor;
        }
        Collections.reverse(order);
        List<RoutePoi> result = new ArrayList<>();
        result.add(routeNodes.get(0));
        result.addAll(order);
        result.add(routeNodes.get(n - 1));
        return result;
    }

    private List<RoutePoi> optimizeByNearestNeighbor(
            List<RoutePoi> routeNodes,
            Map<Long, RoutePoi> pois,
            Map<Long, List<RouteEdge>> graph,
            RouteSearchOptions options) {
        List<RoutePoi> unvisited = new ArrayList<>(routeNodes.subList(1, routeNodes.size() - 1));
        List<RoutePoi> ordered = new ArrayList<>();
        RoutePoi current = routeNodes.get(0);
        ordered.add(current);
        while (!unvisited.isEmpty()) {
            RoutePoi best = null;
            int bestDistance = Integer.MAX_VALUE;
            for (RoutePoi candidate : unvisited) {
                RoutePathResult path = algorithmSelector.findPath(current, candidate, pois, graph, options);
                if (path != null && path.getDistance() < bestDistance) {
                    bestDistance = path.getDistance();
                    best = candidate;
                }
            }
            if (best == null) {
                ordered.addAll(unvisited);
                break;
            }
            ordered.add(best);
            unvisited.remove(best);
            current = best;
        }
        ordered.add(routeNodes.get(routeNodes.size() - 1));
        return twoOpt(ordered, pois, graph, options);
    }

    private List<RoutePoi> twoOpt(
            List<RoutePoi> route,
            Map<Long, RoutePoi> pois,
            Map<Long, List<RouteEdge>> graph,
            RouteSearchOptions options) {
        List<RoutePoi> best = new ArrayList<>(route);
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < best.size() - 2; i++) {
                for (int j = i + 1; j < best.size() - 1; j++) {
                    List<RoutePoi> candidate = new ArrayList<>(best);
                    Collections.reverse(candidate.subList(i, j + 1));
                    if (routeDistance(candidate, pois, graph, options) < routeDistance(best, pois, graph, options)) {
                        best = candidate;
                        improved = true;
                    }
                }
            }
        }
        return best;
    }

    private RoutePathResult[][] pairPaths(
            List<RoutePoi> routeNodes,
            Map<Long, RoutePoi> pois,
            Map<Long, List<RouteEdge>> graph,
            RouteSearchOptions options) {
        int n = routeNodes.size();
        RoutePathResult[][] paths = new RoutePathResult[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    paths[i][j] = algorithmSelector.findPath(routeNodes.get(i), routeNodes.get(j), pois, graph, options);
                }
            }
        }
        return paths;
    }

    private int routeDistance(
            List<RoutePoi> route,
            Map<Long, RoutePoi> pois,
            Map<Long, List<RouteEdge>> graph,
            RouteSearchOptions options) {
        int total = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            RoutePathResult path = algorithmSelector.findPath(route.get(i), route.get(i + 1), pois, graph, options);
            if (path == null) {
                return Integer.MAX_VALUE;
            }
            total += path.getDistance();
        }
        return total;
    }

    @Data
    @AllArgsConstructor
    public static class OrderedRoute {
        private List<RoutePoi> points;
        private String algorithm;
    }
}
