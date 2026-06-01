package com.tourwise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourwise.common.BusinessException;
import com.tourwise.config.AmapProperties;
import com.tourwise.dto.AmapRouteRequest;
import com.tourwise.vo.route.AmapConfigVO;
import com.tourwise.vo.route.RoutePoiVO;
import com.tourwise.vo.route.RoutePointVO;
import com.tourwise.vo.route.RoutePolylinePointVO;
import com.tourwise.vo.route.RouteResultVO;
import com.tourwise.vo.route.RouteStepVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class AmapRouteService {
    private static final String AMAP_BASE = "https://restapi.amap.com/v3/direction/";

    private final AmapProperties amapProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public AmapRouteService(AmapProperties amapProperties) {
        this.amapProperties = amapProperties;
    }

    public AmapConfigVO config() {
        boolean enabled = amapProperties.configured();
        String message = enabled ? "高德地图已配置" : "高德地图未配置，当前仅支持本地路线";
        return new AmapConfigVO(
                enabled,
                enabled ? amapProperties.getJsKey() : null,
                enabled ? amapProperties.getSecurityCode() : null,
                message);
    }

    public RouteResultVO plan(AmapRouteRequest request) {
        ensureConfigured();
        String mode = normalizeMode(request.getMode());
        List<AmapRouteRequest.GeoPoint> points = new ArrayList<>();
        points.add(request.getOrigin());
        if (request.getWaypoints() != null) {
            points.addAll(request.getWaypoints().stream().filter(AmapRouteService::hasCoordinate).toList());
        }
        points.add(request.getDestination());
        if (points.size() < 2) {
            throw BusinessException.badRequest("高德路线至少需要起点和终点");
        }

        int totalDistance = 0;
        int totalDurationSeconds = 0;
        List<RoutePointVO> routePoints = new ArrayList<>();
        List<RoutePolylinePointVO> polyline = new ArrayList<>();
        List<RouteStepVO> steps = new ArrayList<>();
        routePoints.add(pointVO(points.get(0), "从这里出发", 0));

        for (int i = 0; i < points.size() - 1; i++) {
            Segment segment = callAmap(mode, points.get(i), points.get(i + 1));
            totalDistance += segment.getDistance();
            totalDurationSeconds += segment.getDurationSeconds();
            steps.addAll(segment.getSteps());
            appendPolyline(polyline, segment.getPolyline());
            routePoints.add(pointVO(points.get(i + 1), i == points.size() - 2 ? "到达目的地" : "到达途经点", totalDistance));
        }

        RouteResultVO result = new RouteResultVO(
                Math.max(1, (int) Math.ceil(totalDurationSeconds / 60.0)),
                totalDistance,
                routePoints,
                null);
        result.setProvider("amap");
        result.setAlgorithm("amap_" + mode);
        result.setPolyline(polyline);
        result.setSteps(steps);
        return result;
    }

    private Segment callAmap(String mode, AmapRouteRequest.GeoPoint origin, AmapRouteRequest.GeoPoint destination) {
        String url = AMAP_BASE + mode
                + "?key=" + encode(amapProperties.getWebKey())
                + "&origin=" + encode(coordinate(origin))
                + "&destination=" + encode(coordinate(destination))
                + ("driving".equals(mode) ? "&extensions=base" : "");
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw BusinessException.badRequest("高德路线服务请求失败: HTTP " + response.statusCode());
            }
            return parseSegment(response.body());
        } catch (IOException ex) {
            throw BusinessException.badRequest("高德路线服务请求失败");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw BusinessException.badRequest("高德路线服务请求被中断");
        }
    }

    private Segment parseSegment(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (!"1".equals(root.path("status").asText())) {
            String message = root.path("info").asText("高德路线规划失败");
            throw BusinessException.badRequest(message);
        }
        JsonNode path = root.path("route").path("paths").path(0);
        if (path.isMissingNode()) {
            throw BusinessException.badRequest("高德未返回可用路线");
        }
        int distance = path.path("distance").asInt();
        int duration = path.path("duration").asInt();
        List<RouteStepVO> steps = new ArrayList<>();
        List<RoutePolylinePointVO> polyline = new ArrayList<>();
        for (JsonNode step : path.path("steps")) {
            String instruction = stripHtml(step.path("instruction").asText(""));
            int stepDistance = step.path("distance").asInt();
            int stepDuration = step.path("duration").asInt();
            steps.add(new RouteStepVO(instruction, stepDistance,
                    stepDuration <= 0 ? null : Math.max(1, (int) Math.ceil(stepDuration / 60.0))));
            parsePolyline(polyline, step.path("polyline").asText(""));
        }
        return new Segment(distance, duration, steps, polyline);
    }

    private void ensureConfigured() {
        if (!amapProperties.isEnabled() || !StringUtils.hasText(amapProperties.getWebKey())) {
            throw BusinessException.badRequest("高德 Web 服务 Key 未配置");
        }
    }

    private static void parsePolyline(List<RoutePolylinePointVO> points, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        String[] pairs = text.split(";");
        for (String pair : pairs) {
            String[] parts = pair.split(",");
            if (parts.length != 2) {
                continue;
            }
            points.add(new RoutePolylinePointVO(new BigDecimal(parts[0]), new BigDecimal(parts[1]), null, null));
        }
    }

    private static void appendPolyline(List<RoutePolylinePointVO> target, List<RoutePolylinePointVO> source) {
        for (RoutePolylinePointVO point : source) {
            if (target.isEmpty() || !sameCoordinate(target.get(target.size() - 1), point)) {
                target.add(point);
            }
        }
    }

    private static boolean sameCoordinate(RoutePolylinePointVO first, RoutePolylinePointVO second) {
        return first.getLongitude() != null && first.getLatitude() != null
                && first.getLongitude().compareTo(second.getLongitude()) == 0
                && first.getLatitude().compareTo(second.getLatitude()) == 0;
    }

    private static RoutePointVO pointVO(AmapRouteRequest.GeoPoint point, String description, int distance) {
        RoutePoiVO poi = new RoutePoiVO(
                null,
                StringUtils.hasText(point.getName()) ? point.getName() : "路线点",
                "高德路线",
                "amap",
                point.getLongitude(),
                point.getLatitude(),
                null,
                null,
                null);
        return new RoutePointVO(poi, description, distance);
    }

    private static boolean hasCoordinate(AmapRouteRequest.GeoPoint point) {
        return point != null && point.getLongitude() != null && point.getLatitude() != null;
    }

    private static String normalizeMode(String mode) {
        return "driving".equals(mode) ? "driving" : "walking";
    }

    private static String coordinate(AmapRouteRequest.GeoPoint point) {
        return point.getLongitude().toPlainString() + "," + point.getLatitude().toPlainString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String stripHtml(String value) {
        return value == null ? "" : value.replaceAll("<[^>]+>", "");
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class Segment {
        private int distance;
        private int durationSeconds;
        private List<RouteStepVO> steps;
        private List<RoutePolylinePointVO> polyline;
    }
}
