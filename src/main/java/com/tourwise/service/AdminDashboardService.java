package com.tourwise.service;

import com.tourwise.common.MapUtil;
import com.tourwise.mapper.AdminDashboardMapper;
import com.tourwise.model.RouteGraphEdgeRecord;
import com.tourwise.model.RouteGraphNodeRecord;
import com.tourwise.vo.VoConvert;
import com.tourwise.vo.admin.AdminDashboardVO;
import com.tourwise.vo.admin.AdminMetricVO;
import com.tourwise.vo.admin.DataQualityVO;
import com.tourwise.vo.admin.RouteGraphQualityVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Service
public class AdminDashboardService {
    private static final double MAP_PIXEL_TO_METER = 0.35;
    private static final int SUSPICIOUS_EDGE_DISTANCE_M = 800;

    private final AdminDashboardMapper dashboardMapper;
    private final AdminService adminService;

    public AdminDashboardService(AdminDashboardMapper dashboardMapper, AdminService adminService) {
        this.dashboardMapper = dashboardMapper;
        this.adminService = adminService;
    }

    public AdminDashboardVO dashboard() {
        adminService.requireAdmin();
        Map<String, Object> overview = MapUtil.normalize(dashboardMapper.overview());
        List<RouteGraphQualityVO> qualityItems = buildRouteQuality();
        List<DataQualityVO> dataQualityItems = buildDataQuality(qualityItems);

        AdminDashboardVO vo = new AdminDashboardVO();
        vo.setMetrics(buildMetrics(overview, qualityItems, dataQualityItems));
        vo.setDataQuality(dataQualityItems);
        vo.setRouteQuality(qualityItems);
        vo.setIssueCount(issueCount(qualityItems, dataQualityItems));
        vo.setErrorCount(errorCount(qualityItems, dataQualityItems));
        vo.setWarningCount(warningCount(qualityItems, dataQualityItems));
        return vo;
    }

    public List<RouteGraphQualityVO> routeQuality() {
        adminService.requireAdmin();
        return buildRouteQuality();
    }

    private List<AdminMetricVO> buildMetrics(
            Map<String, Object> overview,
            List<RouteGraphQualityVO> qualityItems,
            List<DataQualityVO> dataQualityItems) {
        List<AdminMetricVO> metrics = new ArrayList<>();
        metrics.add(new AdminMetricVO("景点", "spots", intValue(overview, "spotCount"), "已启用大景点数量"));
        metrics.add(new AdminMetricVO("内部路网", "routeScopes", intValue(overview, "routeScopeCount"), "draft / verified 景点"));
        metrics.add(new AdminMetricVO("POI", "pois", intValue(overview, "poiCount"), "可见内部点位数量"));
        metrics.add(new AdminMetricVO("路线边", "routeEdges", intValue(overview, "routeEdgeCount"), "本地路网边数量"));
        metrics.add(new AdminMetricVO("用户", "users", intValue(overview, "userCount"), "正常账号数量"));
        metrics.add(new AdminMetricVO("日志", "logs", intValue(overview, "logCount"), "正常游记/圈子日志"));
        metrics.add(new AdminMetricVO("圈子", "circles", intValue(overview, "circleCount"), "正常圈子数量"));
        metrics.add(new AdminMetricVO("质检问题", "qualityIssues",
                issueCount(qualityItems, dataQualityItems),
                "景点和路网数据检查发现的问题"));
        return metrics;
    }

    private List<DataQualityVO> buildDataQuality(List<RouteGraphQualityVO> routeQualityItems) {
        Map<Long, RouteGraphQualityVO> routeQualityByGroup = new HashMap<>();
        for (RouteGraphQualityVO routeQuality : routeQualityItems) {
            routeQualityByGroup.put(routeQuality.getPlaceGroupId(), routeQuality);
        }
        return dashboardMapper.listSpotQuality()
                .stream()
                .map(MapUtil::normalize)
                .map(DataQualityVO::from)
                .peek(item -> enrichDataQuality(item, routeQualityByGroup.get(item.getPlaceGroupId())))
                .sorted(Comparator
                        .comparing((DataQualityVO item) -> levelRank(item.getLevel()))
                        .thenComparing(DataQualityVO::getSpotId))
                .toList();
    }

    private void enrichDataQuality(DataQualityVO item, RouteGraphQualityVO routeQuality) {
        List<String> issues = new ArrayList<>();
        if (Boolean.TRUE.equals(item.getMissingRepresentativePoi())) {
            issues.add("缺少代表 POI，前台详情/推荐可能无法跳转");
        }
        if (Boolean.TRUE.equals(item.getRepresentativePoiDisabled())) {
            issues.add("代表 POI 已停用，前台可能搜不到该景点");
        }
        if (Boolean.TRUE.equals(item.getMissingGeo())) {
            issues.add("缺少经纬度，无法做高德路线和定位匹配");
        }
        if (Boolean.TRUE.equals(item.getMissingShortName())) {
            issues.add("缺少简称，搜索“北邮/北航”这类简称时命中率会下降");
        }
        if (Boolean.TRUE.equals(item.getMissingCover())) {
            issues.add("缺少封面图，首页/推荐卡片会显示占位图");
        }
        if (Boolean.TRUE.equals(item.getMissingCategory())) {
            issues.add("缺少分类，标签筛选和推荐排序会受影响");
        }
        if (Boolean.TRUE.equals(item.getMissingTags())) {
            issues.add("缺少景点标签，个性化推荐缺少匹配依据");
        }
        if (Boolean.TRUE.equals(item.getMissingDescription())) {
            issues.add("缺少介绍文案，景点详情页内容偏空");
        }
        if (Boolean.TRUE.equals(item.getMissingAddress())) {
            issues.add("缺少地址，用户难以确认真实位置");
        }
        if (Boolean.TRUE.equals(item.getMissingVisiblePoi())) {
            issues.add("没有可见 POI，内部路线和详情内容不完整");
        }
        if (Boolean.TRUE.equals(item.getCampusRouteMissing())) {
            issues.add("高校/校区未配置内部路网状态");
        }
        if (routeQuality != null && routeQuality.getIssues() != null && !routeQuality.getIssues().isEmpty()) {
            item.setRouteIssueCount(routeQuality.getIssues().size());
            issues.add("内部路网存在 " + routeQuality.getIssues().size() + " 个问题");
        }
        item.setIssues(issues);
        if (Boolean.TRUE.equals(item.getMissingRepresentativePoi())
                || Boolean.TRUE.equals(item.getRepresentativePoiDisabled())
                || Boolean.TRUE.equals(item.getMissingGeo())
                || Boolean.TRUE.equals(item.getMissingVisiblePoi())
                || (routeQuality != null && "error".equals(routeQuality.getLevel()))) {
            item.setLevel("error");
        } else if (!issues.isEmpty()) {
            item.setLevel("warning");
        } else {
            item.setLevel("ok");
        }
        fillCompleteness(item, routeQuality);
    }

    private void fillCompleteness(DataQualityVO item, RouteGraphQualityVO routeQuality) {
        int total = 0;
        int completed = 0;

        total++;
        if (!Boolean.TRUE.equals(item.getMissingCover())) completed++;
        total++;
        if (!Boolean.TRUE.equals(item.getMissingGeo())) completed++;
        total++;
        if (!Boolean.TRUE.equals(item.getMissingCategory())) completed++;
        total++;
        if (!Boolean.TRUE.equals(item.getMissingTags())) completed++;
        total++;
        if (!Boolean.TRUE.equals(item.getMissingRepresentativePoi())
                && !Boolean.TRUE.equals(item.getRepresentativePoiDisabled())) completed++;
        total++;
        if (!Boolean.TRUE.equals(item.getMissingVisiblePoi())) completed++;
        total++;
        if (!Boolean.TRUE.equals(item.getMissingShortName())) completed++;
        total++;
        if (!Boolean.TRUE.equals(item.getMissingDescription())) completed++;
        total++;
        if (!Boolean.TRUE.equals(item.getMissingAddress())) completed++;

        if (requiresInternalRoute(item)) {
            total++;
            if (!Boolean.TRUE.equals(item.getCampusRouteMissing())) completed++;
            total++;
            if (routeQuality != null && !"error".equals(routeQuality.getLevel())) completed++;
        }

        item.setCompletedItemCount(completed);
        item.setTotalItemCount(total);
        item.setCompletenessScore(total <= 0 ? 0 : Math.round(completed * 100.0f / total));
    }

    private boolean requiresInternalRoute(DataQualityVO item) {
        String spotType = item.getSpotType();
        return "campus".equals(spotType)
                || "university".equals(spotType)
                || !"none".equals(item.getRouteGraphStatus());
    }

    private int issueCount(List<RouteGraphQualityVO> routeQualityItems, List<DataQualityVO> dataQualityItems) {
        return routeQualityItems.stream().mapToInt(item -> item.getIssues().size()).sum()
                + dataQualityItems.stream().mapToInt(item -> item.getIssues().size()).sum();
    }

    private int errorCount(List<RouteGraphQualityVO> routeQualityItems, List<DataQualityVO> dataQualityItems) {
        return (int) routeQualityItems.stream().filter(item -> "error".equals(item.getLevel())).count()
                + (int) dataQualityItems.stream().filter(item -> "error".equals(item.getLevel())).count();
    }

    private int warningCount(List<RouteGraphQualityVO> routeQualityItems, List<DataQualityVO> dataQualityItems) {
        return (int) routeQualityItems.stream().filter(item -> "warning".equals(item.getLevel())).count()
                + (int) dataQualityItems.stream().filter(item -> "warning".equals(item.getLevel())).count();
    }

    private int levelRank(String level) {
        if ("error".equals(level)) {
            return 0;
        }
        if ("warning".equals(level)) {
            return 1;
        }
        return 2;
    }

    private List<RouteGraphQualityVO> buildRouteQuality() {
        return dashboardMapper.listRouteGraphScopes()
                .stream()
                .map(MapUtil::normalize)
                .map(this::buildQualityItem)
                .sorted(Comparator.comparing(RouteGraphQualityVO::getPlaceGroupId))
                .toList();
    }

    private RouteGraphQualityVO buildQualityItem(Map<String, Object> scope) {
        Long placeGroupId = VoConvert.longValue(scope, "placeGroupId");
        List<RouteGraphNodeRecord> nodes = dashboardMapper.listQualityNodes(placeGroupId);
        List<RouteGraphEdgeRecord> edges = dashboardMapper.listQualityEdges(placeGroupId);

        Map<Long, RouteGraphNodeRecord> nodeMap = new HashMap<>();
        nodes.stream()
                .filter(node -> node.getId() != null)
                .forEach(node -> nodeMap.put(node.getId(), node));

        Set<Long> visibleNodeIds = new LinkedHashSet<>();
        Set<Long> routeNodeIds = new LinkedHashSet<>();
        int missingGeoCount = 0;
        int missingMapPointCount = 0;
        for (RouteGraphNodeRecord node : nodes) {
            if (isRouteNode(node)) {
                routeNodeIds.add(node.getId());
                continue;
            }
            visibleNodeIds.add(node.getId());
            if (node.getLongitude() == null || node.getLatitude() == null) {
                missingGeoCount++;
            }
            if (node.getMapX() == null || node.getMapY() == null) {
                missingMapPointCount++;
            }
        }

        Map<Long, Set<Long>> graph = new HashMap<>();
        Map<Long, Integer> degree = new HashMap<>();
        int suspiciousEdgeCount = 0;
        for (RouteGraphEdgeRecord edge : edges) {
            Long fromId = edge.getFromPoiId();
            Long toId = edge.getToPoiId();
            if (fromId == null || toId == null || fromId.equals(toId)
                    || !nodeMap.containsKey(fromId) || !nodeMap.containsKey(toId)
                    || edge.getDistanceM() == null || edge.getDistanceM() <= 0) {
                suspiciousEdgeCount++;
                continue;
            }
            graph.computeIfAbsent(fromId, key -> new LinkedHashSet<>()).add(toId);
            graph.computeIfAbsent(toId, key -> new LinkedHashSet<>()).add(fromId);
            degree.merge(fromId, 1, Integer::sum);
            degree.merge(toId, 1, Integer::sum);
            if (isSuspiciousLongEdge(edge, nodeMap.get(fromId), nodeMap.get(toId))) {
                suspiciousEdgeCount++;
            }
        }

        int isolatedPoiCount = (int) visibleNodeIds.stream()
                .filter(id -> degree.getOrDefault(id, 0) == 0)
                .count();
        int isolatedRouteNodeCount = (int) routeNodeIds.stream()
                .filter(id -> degree.getOrDefault(id, 0) == 0)
                .count();
        int unreachablePoiCount = unreachableVisiblePoiCount(visibleNodeIds, graph);

        RouteGraphQualityVO vo = new RouteGraphQualityVO();
        vo.setPlaceGroupId(placeGroupId);
        vo.setName(VoConvert.string(scope, "name"));
        vo.setRouteGraphStatus(VoConvert.string(scope, "routeGraphStatus"));
        vo.setHasMap(VoConvert.intValue(scope, "mapCount") != null && VoConvert.intValue(scope, "mapCount") > 0);
        vo.setMapWidth(VoConvert.intValue(scope, "mapWidth"));
        vo.setMapHeight(VoConvert.intValue(scope, "mapHeight"));
        vo.setVisiblePoiCount(visibleNodeIds.size());
        vo.setRouteNodeCount(routeNodeIds.size());
        vo.setEdgeCount(edges.size());
        vo.setMissingGeoCount(missingGeoCount);
        vo.setMissingMapPointCount(missingMapPointCount);
        vo.setIsolatedPoiCount(isolatedPoiCount);
        vo.setIsolatedRouteNodeCount(isolatedRouteNodeCount);
        vo.setUnreachablePoiCount(unreachablePoiCount);
        vo.setSuspiciousEdgeCount(suspiciousEdgeCount);
        vo.setIssues(buildIssues(vo));
        vo.setLevel(resolveLevel(vo));
        return vo;
    }

    private List<String> buildIssues(RouteGraphQualityVO item) {
        List<String> issues = new ArrayList<>();
        if (!Boolean.TRUE.equals(item.getHasMap())) {
            issues.add("缺少平面图底图");
        }
        if (item.getVisiblePoiCount() == 0) {
            issues.add("没有可选 POI");
        }
        if (item.getMissingMapPointCount() > 0) {
            issues.add(item.getMissingMapPointCount() + " 个 POI 缺少平面图坐标");
        }
        if (item.getMissingGeoCount() > 0) {
            issues.add(item.getMissingGeoCount() + " 个 POI 缺少经纬度");
        }
        if (item.getEdgeCount() == 0) {
            issues.add("没有路线边，无法进行内部路线规划");
        }
        if (item.getIsolatedPoiCount() > 0) {
            issues.add(item.getIsolatedPoiCount() + " 个 POI 没有接入路网");
        }
        if (item.getIsolatedRouteNodeCount() > 0) {
            issues.add(item.getIsolatedRouteNodeCount() + " 个隐藏路口没有连接任何路线");
        }
        if (item.getUnreachablePoiCount() > 0) {
            issues.add(item.getUnreachablePoiCount() + " 个 POI 不在主连通路网中");
        }
        if (item.getSuspiciousEdgeCount() > 0) {
            issues.add(item.getSuspiciousEdgeCount() + " 条路线边疑似异常");
        }
        return issues;
    }

    private String resolveLevel(RouteGraphQualityVO item) {
        if (!Boolean.TRUE.equals(item.getHasMap())
                || item.getMissingMapPointCount() > 0
                || item.getVisiblePoiCount() == 0
                || item.getUnreachablePoiCount() > 0) {
            return "error";
        }
        if (item.getEdgeCount() == 0
                || item.getMissingGeoCount() > 0
                || item.getIsolatedPoiCount() > 0
                || item.getIsolatedRouteNodeCount() > 0
                || item.getSuspiciousEdgeCount() > 0) {
            return "warning";
        }
        return "ok";
    }

    private int unreachableVisiblePoiCount(Set<Long> visibleNodeIds, Map<Long, Set<Long>> graph) {
        if (visibleNodeIds.isEmpty()) {
            return 0;
        }
        Set<Long> visited = new HashSet<>();
        int largestVisibleComponent = 0;
        for (Long nodeId : visibleNodeIds) {
            if (visited.contains(nodeId)) {
                continue;
            }
            Set<Long> component = bfs(nodeId, graph, visited);
            int visibleCount = (int) component.stream().filter(visibleNodeIds::contains).count();
            largestVisibleComponent = Math.max(largestVisibleComponent, visibleCount);
        }
        return Math.max(0, visibleNodeIds.size() - largestVisibleComponent);
    }

    private Set<Long> bfs(Long start, Map<Long, Set<Long>> graph, Set<Long> globalVisited) {
        Set<Long> component = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(start);
        globalVisited.add(start);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            component.add(current);
            for (Long next : graph.getOrDefault(current, Set.of())) {
                if (globalVisited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return component;
    }

    private boolean isSuspiciousLongEdge(
            RouteGraphEdgeRecord edge,
            RouteGraphNodeRecord from,
            RouteGraphNodeRecord to) {
        if (edge.getDistanceM() != null && edge.getDistanceM() > SUSPICIOUS_EDGE_DISTANCE_M) {
            return true;
        }
        if (hasMapPoint(from) && hasMapPoint(to) && edge.getDistanceM() != null) {
            double dx = from.getMapX() - to.getMapX();
            double dy = from.getMapY() - to.getMapY();
            int estimated = Math.max(1, (int) Math.round(Math.sqrt(dx * dx + dy * dy) * MAP_PIXEL_TO_METER));
            BigDecimal savedDistance = BigDecimal.valueOf(edge.getDistanceM());
            BigDecimal estimatedDistance = BigDecimal.valueOf(estimated);
            return savedDistance.compareTo(estimatedDistance.multiply(BigDecimal.valueOf(4))) > 0
                    || savedDistance.multiply(BigDecimal.valueOf(4)).compareTo(estimatedDistance) < 0;
        }
        return false;
    }

    private boolean isRouteNode(RouteGraphNodeRecord node) {
        return node != null && "route".equals(node.getAreaCode());
    }

    private boolean hasMapPoint(RouteGraphNodeRecord node) {
        return node != null && node.getMapX() != null && node.getMapY() != null;
    }

    private static int intValue(Map<String, Object> row, String key) {
        Integer value = VoConvert.intValue(row, key);
        return value == null ? 0 : value;
    }
}
