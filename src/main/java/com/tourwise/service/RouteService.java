package com.tourwise.service;

import com.tourwise.dto.*;
import com.tourwise.mapper.*;
import com.tourwise.model.*;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.service.route.MultiPointRoutePlanner;
import com.tourwise.service.route.RouteAlgorithmSelector;
import com.tourwise.service.route.RoutePathResult;
import com.tourwise.service.route.RouteSearchOptions;
import com.tourwise.vo.route.RoutePoiVO;
import com.tourwise.vo.route.RoutePointVO;
import com.tourwise.vo.route.RoutePolylinePointVO;
import com.tourwise.vo.route.RouteResultVO;
import com.tourwise.vo.route.RouteScopeVO;
import com.tourwise.vo.route.RouteSpotVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RouteService {
    private final RouteMapper routeMapper;
    private final RouteAlgorithmSelector algorithmSelector;
    private final MultiPointRoutePlanner multiPointRoutePlanner;

    public RouteService(
            RouteMapper routeMapper,
            RouteAlgorithmSelector algorithmSelector,
            MultiPointRoutePlanner multiPointRoutePlanner) {
        this.routeMapper = routeMapper;
        this.algorithmSelector = algorithmSelector;
        this.multiPointRoutePlanner = multiPointRoutePlanner;
    }

    public List<RouteScopeVO> scopes() {
        return routeMapper.listRouteScopes().stream().map(MapUtil::normalize).map(RouteScopeVO::from).toList();
    }

    public List<RouteSpotVO> spots() {
        return routeMapper.listRouteSpots().stream().map(MapUtil::normalize).map(RouteSpotVO::from).toList();
    }

    public List<RoutePoiVO> pois(String keyword, String category, Long spotId, Long placeGroupId) {
        Long scopePlaceGroupId = resolveScopePlaceGroupId(spotId, placeGroupId);
        if (scopePlaceGroupId == null) {
            return List.of();
        }
        return routeMapper.listPois(trimToNull(keyword), trimToNull(category), scopePlaceGroupId)
                .stream()
                .map(MapUtil::normalize)
                .map(RoutePoiVO::from)
                .toList();
    }

    public RouteResultVO shortest(RouteRequest request, boolean indoorOnly) {
        Long scopePlaceGroupId = resolveScopePlaceGroupId(request.getSpotId(), request.getPlaceGroupId());
        if (scopePlaceGroupId == null) {
            throw BusinessException.badRequest("请先选择具体景点后再进行路线规划");
        }
        List<String> points = new ArrayList<>();
        points.add(request.getStart());
        if (request.getWaypoints() != null) {
            points.addAll(request.getWaypoints().stream().filter(StringUtils::hasText).map(String::trim).toList());
        }
        points.add(request.getEnd());
        return planByPoints(points, request.getPreferences(), indoorOnly, scopePlaceGroupId, false);
    }

    public RouteResultVO optimal(OptimalRouteRequest request) {
        Long scopePlaceGroupId = resolveScopePlaceGroupId(request.getSpotId(), request.getPlaceGroupId());
        if (scopePlaceGroupId == null) {
            throw BusinessException.badRequest("请先选择具体景点后再进行路线规划");
        }
        List<String> points = request.getPoints().stream().filter(StringUtils::hasText).map(String::trim).toList();
        if (points.size() < 2) {
            throw BusinessException.badRequest("至少需要两个路线点");
        }
        return planByPoints(points, request.getPreferences(), false, scopePlaceGroupId, true);
    }

    private RouteResultVO planByPoints(
            List<String> rawPoints,
            List<String> preferences,
            boolean indoorOnly,
            Long scopePlaceGroupId,
            boolean optimizeOrder) {
        RouteSearchOptions options = new RouteSearchOptions(preferences);
        Map<Long, RoutePoi> pois = graphPois(scopePlaceGroupId);
        Map<Long, List<RouteEdge>> graph = buildGraph(indoorOnly, scopePlaceGroupId);
        if (graph.isEmpty()) {
            return emptyRoute(scopePlaceGroupId == null ? "暂无可用路线边数据" : "当前地点暂无可规划路线数据", "dijkstra");
        }

        List<RoutePoi> routeNodes = rawPoints.stream()
                .map(point -> resolvePoi(point, scopePlaceGroupId))
                .toList();
        String routeAlgorithm = null;
        if (optimizeOrder) {
            MultiPointRoutePlanner.OrderedRoute orderedRoute = multiPointRoutePlanner.optimize(routeNodes, pois, graph, options);
            routeNodes = orderedRoute.getPoints();
            routeAlgorithm = orderedRoute.getAlgorithm();
        }
        int totalDistance = 0;
        int totalDuration = 0;
        List<RoutePointVO> points = new ArrayList<>();
        List<RoutePolylinePointVO> polyline = new ArrayList<>();

        for (int i = 0; i < routeNodes.size() - 1; i++) {
            RoutePoi start = routeNodes.get(i);
            RoutePoi end = routeNodes.get(i + 1);
            RoutePathResult segment = algorithmSelector.findPath(start, end, pois, graph, options);
            if (segment == null) {
                if (indoorOnly) {
                    return emptyRoute("暂无室内路线数据", "dijkstra");
                }
                throw BusinessException.notFound("起点或终点之间暂无可规划路线");
            }
            if (routeAlgorithm == null || ("astar".equals(routeAlgorithm) && !"astar".equals(segment.getAlgorithm()))) {
                routeAlgorithm = segment.getAlgorithm();
            }
            totalDistance += segment.getDistance();
            totalDuration += segment.getDuration();
            appendPoints(points, polyline, segment, pois, i == 0);
        }
        if (!points.isEmpty()) {
            points.get(points.size() - 1).setDescription("到达目的地");
        }

        RouteResultVO result = new RouteResultVO(totalDuration, totalDistance, points, null);
        result.setProvider("local");
        result.setAlgorithm(routeAlgorithm == null ? "dijkstra" : routeAlgorithm);
        result.setPolyline(polyline);
        return result;
    }

    private RoutePoi resolvePoi(String input, Long scopePlaceGroupId) {
        String text = input == null ? "" : input.trim();
        if (!StringUtils.hasText(text)) {
            throw BusinessException.badRequest("路线点不能为空");
        }
        RoutePoi poi = null;
        if (text.matches("\\d+")) {
            poi = routeMapper.findPoiById(Long.parseLong(text), scopePlaceGroupId);
        } else if (text.matches("-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?")) {
            String[] parts = text.split(",");
            poi = routeMapper.findNearestPoi(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()), scopePlaceGroupId);
        } else {
            poi = routeMapper.findPoiByExactName(text, scopePlaceGroupId);
            if (poi == null) {
                poi = routeMapper.findPoiByLikeName(text, scopePlaceGroupId);
            }
        }
        if (poi == null) {
            throw BusinessException.notFound("路线点无法匹配到 POI: " + text);
        }
        return poi;
    }

    private Map<Long, List<RouteEdge>> buildGraph(boolean indoorOnly, Long scopePlaceGroupId) {
        Map<Long, List<RouteEdge>> graph = new HashMap<>();
        for (RouteEdge edge : routeMapper.listEdges(indoorOnly, scopePlaceGroupId)) {
            graph.computeIfAbsent(edge.getFromPoiId(), key -> new ArrayList<>()).add(edge);
        }
        return graph;
    }

    private Map<Long, RoutePoi> graphPois(Long scopePlaceGroupId) {
        Map<Long, RoutePoi> result = new HashMap<>();
        for (RoutePoi poi : routeMapper.listGraphPois(scopePlaceGroupId)) {
            result.put(poi.getId(), poi);
        }
        return result;
    }

    private Long resolveScopePlaceGroupId(Long spotId, Long placeGroupId) {
        if (spotId != null && spotId > 0) {
            Long resolved = routeMapper.findRouteScopeByPoiId(spotId);
            if (resolved != null && resolved > 0) {
                return resolved;
            }
        }
        if (placeGroupId != null && placeGroupId > 0) {
            return placeGroupId;
        }
        return null;
    }

    private static void appendPoints(
            List<RoutePointVO> points,
            List<RoutePolylinePointVO> polyline,
            RoutePathResult segment,
            Map<Long, RoutePoi> pois,
            boolean firstSegment) {
        int cumulativeDistance = points.isEmpty() ? 0 : points.get(points.size() - 1).getDistance();
        if (firstSegment) {
            RoutePoi start = pois.get(segment.getStartId());
            RoutePointVO startPoint = pointVO(start, "从这里出发", cumulativeDistance);
            points.add(startPoint);
            polyline.add(polylinePoint(start));
        }
        for (RouteEdge edge : segment.getEdges()) {
            cumulativeDistance += edge.getDistanceM();
            RoutePoi poi = pois.get(edge.getToPoiId());
            if (poi == null) {
                continue;
            }
            points.add(pointVO(poi, edge.getDescription(), cumulativeDistance));
            polyline.add(polylinePoint(poi));
        }
    }

    private static RoutePointVO pointVO(RoutePoi poi, String description, int distance) {
        if (poi == null) {
            return new RoutePointVO();
        }
        RoutePoiVO poiVO = new RoutePoiVO(
                poi.getId(),
                poi.getName(),
                poi.getCategory(),
                poi.getType(),
                poi.getLongitude(),
                poi.getLatitude(),
                poi.getMapX(),
                poi.getMapY(),
                poi.getPlaceGroupId());
        return new RoutePointVO(poiVO, description, distance);
    }

    private static RoutePolylinePointVO polylinePoint(RoutePoi poi) {
        if (poi == null) {
            return new RoutePolylinePointVO();
        }
        return new RoutePolylinePointVO(poi.getLongitude(), poi.getLatitude(), poi.getMapX(), poi.getMapY());
    }

    private static RouteResultVO emptyRoute(String message, String algorithm) {
        RouteResultVO result = new RouteResultVO(0, 0, List.of(), message);
        result.setProvider("local");
        result.setAlgorithm(algorithm);
        result.setPolyline(List.of());
        return result;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

}
