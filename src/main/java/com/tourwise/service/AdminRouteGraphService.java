package com.tourwise.service;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.dto.RouteGraphVersionCreateRequest;
import com.tourwise.dto.RouteGraphEdgeRequest;
import com.tourwise.dto.RouteGraphNodeRequest;
import com.tourwise.dto.RouteGraphSaveRequest;
import com.tourwise.dto.RouteGraphSaveWithVersionRequest;
import com.tourwise.mapper.AdminRouteGraphMapper;
import com.tourwise.mapper.RouteMapMapper;
import com.tourwise.model.RouteEdge;
import com.tourwise.model.RouteGraphEdgeRecord;
import com.tourwise.model.RouteGraphNodeRecord;
import com.tourwise.model.RouteGraphVersionRecord;
import com.tourwise.vo.route.RouteGraphEdgeVO;
import com.tourwise.vo.route.RouteGraphExportStatsVO;
import com.tourwise.vo.route.RouteGraphDiffEdgeVO;
import com.tourwise.vo.route.RouteGraphDiffNodeVO;
import com.tourwise.vo.route.RouteGraphDiffVO;
import com.tourwise.vo.route.RouteGraphNodeVO;
import com.tourwise.vo.route.RouteGraphCleanupVO;
import com.tourwise.vo.route.RouteGraphIssueVO;
import com.tourwise.vo.route.RouteGraphInspectionVO;
import com.tourwise.vo.route.RouteGraphVO;
import com.tourwise.vo.route.RouteMapVO;
import com.tourwise.vo.route.RouteGraphVersionVO;
import com.tourwise.vo.VoConvert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminRouteGraphService {
    private static final double MAP_PIXEL_TO_METER = 0.35;
    private static final BigDecimal BASE_LONGITUDE = new BigDecimal("116.286500");
    private static final BigDecimal BASE_LATITUDE = new BigDecimal("40.158800");
    private static final BigDecimal LONGITUDE_PER_PIXEL = new BigDecimal("0.00000265");
    private static final BigDecimal LATITUDE_PER_PIXEL = new BigDecimal("0.00000220");
    private static final int LONG_EDGE_WARNING_DISTANCE_M = 260;

    private final AdminRouteGraphMapper routeGraphMapper;
    private final RouteMapMapper routeMapMapper;
    private final AdminService adminService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminRouteGraphService(
            AdminRouteGraphMapper routeGraphMapper,
            RouteMapMapper routeMapMapper,
            AdminService adminService) {
        this.routeGraphMapper = routeGraphMapper;
        this.routeMapMapper = routeMapMapper;
        this.adminService = adminService;
    }

    public RouteGraphVO graph(Long placeGroupId) {
        adminService.requireAdmin();
        validatePlaceGroup(placeGroupId);
        RouteGraphVO vo = new RouteGraphVO();
        vo.setPlaceGroupId(placeGroupId);
        vo.setMap(RouteMapVO.from(routeMapMapper.findByPlaceGroupId(placeGroupId)));
        vo.setNodes(routeGraphMapper.listNodes(placeGroupId).stream().map(RouteGraphNodeVO::from).toList());
        vo.setEdges(routeGraphMapper.listEdges(placeGroupId).stream().map(RouteGraphEdgeVO::from).toList());
        return vo;
    }

    public List<RouteGraphVersionVO> versions(Long placeGroupId) {
        adminService.requireAdmin();
        validatePlaceGroup(placeGroupId);
        return routeGraphMapper.listVersions(placeGroupId).stream().map(RouteGraphVersionVO::from).toList();
    }

    public RouteGraphSaveRequest versionSnapshot(Long placeGroupId, Long versionId) {
        adminService.requireAdmin();
        RouteGraphVersionRecord version = findExistingVersion(placeGroupId, versionId);
        RouteGraphSaveRequest snapshot = fromJson(version.getSnapshotJson());
        if (snapshot.getStats() == null) {
            fillExportMetadata(
                    snapshot,
                    placeGroupId,
                    snapshot.getNodes() == null ? List.of() : snapshot.getNodes(),
                    snapshot.getEdges() == null ? List.of() : snapshot.getEdges());
        }
        return snapshot;
    }

    public RouteGraphDiffVO diffVersion(Long placeGroupId, Long versionId) {
        adminService.requireAdmin();
        RouteGraphVersionRecord version = findExistingVersion(placeGroupId, versionId);
        RouteGraphSaveRequest current = buildSnapshot(placeGroupId);
        RouteGraphSaveRequest target = fromJson(version.getSnapshotJson());
        if (target.getNodes() == null) {
            target.setNodes(List.of());
        }
        if (target.getEdges() == null) {
            target.setEdges(List.of());
        }
        if (target.getStats() == null) {
            fillExportMetadata(target, placeGroupId, target.getNodes(), target.getEdges());
        }

        RouteGraphDiffVO diff = new RouteGraphDiffVO();
        diff.setPlaceGroupId(placeGroupId);
        diff.setVersion(RouteGraphVersionVO.from(version));
        diff.setCurrentStats(current.getStats());
        diff.setVersionStats(target.getStats());
        diff.setWarnings(buildDiffWarnings(current, target));

        Map<String, RouteGraphNodeRequest> currentNodes = nodeIdentityMap(current.getNodes());
        Map<String, RouteGraphNodeRequest> targetNodes = nodeIdentityMap(target.getNodes());

        List<RouteGraphDiffNodeVO> addedNodes = new ArrayList<>();
        List<RouteGraphDiffNodeVO> removedNodes = new ArrayList<>();
        List<RouteGraphDiffNodeVO> movedNodes = new ArrayList<>();
        for (Map.Entry<String, RouteGraphNodeRequest> entry : targetNodes.entrySet()) {
            RouteGraphNodeRequest currentNode = currentNodes.get(entry.getKey());
            RouteGraphNodeRequest targetNode = entry.getValue();
            if (currentNode == null) {
                addedNodes.add(nodeDiff(targetNode, null, targetNode, "added"));
            } else if (isPoiNode(targetNode) && coordinatesChanged(currentNode, targetNode)) {
                movedNodes.add(nodeDiff(targetNode, currentNode, targetNode, "moved"));
            }
        }
        for (Map.Entry<String, RouteGraphNodeRequest> entry : currentNodes.entrySet()) {
            if (!targetNodes.containsKey(entry.getKey())) {
                RouteGraphNodeRequest currentNode = entry.getValue();
                removedNodes.add(nodeDiff(currentNode, currentNode, null, "removed"));
            }
        }

        Map<String, RouteGraphDiffEdgeVO> currentEdges = edgeIdentityMap(current);
        Map<String, RouteGraphDiffEdgeVO> targetEdges = edgeIdentityMap(target);
        List<RouteGraphDiffEdgeVO> addedEdges = new ArrayList<>();
        List<RouteGraphDiffEdgeVO> removedEdges = new ArrayList<>();
        for (Map.Entry<String, RouteGraphDiffEdgeVO> entry : targetEdges.entrySet()) {
            if (!currentEdges.containsKey(entry.getKey())) {
                RouteGraphDiffEdgeVO edge = entry.getValue();
                edge.setChangeType("added");
                addedEdges.add(edge);
            }
        }
        for (Map.Entry<String, RouteGraphDiffEdgeVO> entry : currentEdges.entrySet()) {
            if (!targetEdges.containsKey(entry.getKey())) {
                RouteGraphDiffEdgeVO edge = entry.getValue();
                edge.setChangeType("removed");
                removedEdges.add(edge);
            }
        }

        diff.setAddedNodes(addedNodes);
        diff.setRemovedNodes(removedNodes);
        diff.setMovedNodes(movedNodes);
        diff.setAddedEdges(addedEdges);
        diff.setRemovedEdges(removedEdges);
        return diff;
    }

    public RouteGraphInspectionVO quality(Long placeGroupId) {
        adminService.requireAdmin();
        validatePlaceGroup(placeGroupId);

        RouteGraphInspectionVO vo = new RouteGraphInspectionVO();
        vo.setPlaceGroupId(placeGroupId);

        Map<String, Object> overview = MapUtil.normalize(routeGraphMapper.qualityOverview(placeGroupId));
        vo.setPoiCount(intValue(overview, "poiCount"));
        vo.setRouteNodeCount(intValue(overview, "routeNodeCount"));
        vo.setEdgeCount(intValue(overview, "edgeCount"));

        List<RouteGraphIssueVO> issues = new ArrayList<>();
        routeGraphMapper.listMissingMapPointPois(placeGroupId)
                .forEach(row -> issues.add(nodeIssue(
                        row,
                        "missingMapPoint",
                        "error",
                        "POI 缺少平面图坐标",
                        "这个 POI 没有 map_x/map_y，无法在平面图上参与内部路线规划。")));
        routeGraphMapper.listIsolatedPois(placeGroupId)
                .forEach(row -> issues.add(nodeIssue(
                        row,
                        "isolatedPoi",
                        "error",
                        "POI 未接入路网",
                        "这个 POI 已经有平面图坐标，但没有任何路线边连接，用户选它作为起终点时大概率规划失败。")));
        routeGraphMapper.listIsolatedRouteNodes(placeGroupId)
                .forEach(row -> issues.add(nodeIssue(
                        row,
                        "isolatedRouteNode",
                        "warning",
                        "隐藏路口未接入路网",
                        "这个隐藏路口没有连接任何路线边，通常是误点后忘记删除。")));
        routeGraphMapper.listInvalidEdges(placeGroupId)
                .forEach(row -> issues.add(edgeIssue(
                        row,
                        "invalidEdge",
                        "error",
                        "路线边引用了无效节点",
                        "这条路线边连接了不存在或已停用的 POI，需要删除后重新连接。")));
        routeGraphMapper.listCrossGroupEdges(placeGroupId)
                .forEach(row -> issues.add(edgeIssue(
                        row,
                        "crossGroupEdge",
                        "error",
                        "路线边跨越了不同景点",
                        "内部路线边只能连接同一个景点/校区下的 POI 或路口。")));
        routeGraphMapper.listSelfLoopEdges(placeGroupId)
                .forEach(row -> issues.add(edgeIssue(
                        row,
                        "selfLoopEdge",
                        "error",
                        "路线边起点终点相同",
                        "这条路线边是自环，没有实际导航意义。")));
        routeGraphMapper.listDuplicateDirectedEdges(placeGroupId)
                .forEach(row -> issues.add(edgeIssue(
                        row,
                        "duplicateEdge",
                        "warning",
                        "存在重复路线边",
                        "同方向的两个节点之间存在多条重复边，建议清理后只保留一条。")));
        routeGraphMapper.listLongEdges(placeGroupId, LONG_EDGE_WARNING_DISTANCE_M)
                .forEach(row -> issues.add(edgeIssue(
                        row,
                        "longEdge",
                        "warning",
                        "路线边距离偏长",
                        "这条边距离超过 " + LONG_EDGE_WARNING_DISTANCE_M + " 米，建议确认是否穿过建筑、操场或湖泊。")));

        vo.setIssues(issues);
        vo.setIssueCount(issues.size());
        vo.setBlockingIssueCount((int) issues.stream().filter(issue -> "error".equals(issue.getSeverity())).count());
        return vo;
    }

    @Transactional
    public RouteGraphCleanupVO cleanupQualityIssues(Long placeGroupId) {
        adminService.requireAdmin();
        validatePlaceGroup(placeGroupId);

        int invalidCandidateCount = routeGraphMapper.listInvalidEdges(placeGroupId).size();
        int crossGroupCandidateCount = routeGraphMapper.listCrossGroupEdges(placeGroupId).size();
        int selfLoopCandidateCount = routeGraphMapper.listSelfLoopEdges(placeGroupId).size();
        int duplicateCandidateCount = routeGraphMapper.listDuplicateDirectedEdges(placeGroupId).size();
        int candidateCount = invalidCandidateCount
                + crossGroupCandidateCount
                + selfLoopCandidateCount
                + duplicateCandidateCount;

        RouteGraphCleanupVO vo = new RouteGraphCleanupVO();
        vo.setPlaceGroupId(placeGroupId);
        if (candidateCount <= 0) {
            vo.setInvalidEdgeCount(0);
            vo.setCrossGroupEdgeCount(0);
            vo.setSelfLoopEdgeCount(0);
            vo.setDuplicateEdgeCount(0);
            vo.setTotalCount(0);
            vo.setMessage("没有发现可自动清理的路线边");
            vo.setQuality(quality(placeGroupId));
            return vo;
        }

        createAutoVersion(placeGroupId, "清理前自动备份", "自动清理无效边、跨景点边、自环边和重复边前创建");

        int invalidEdgeCount = routeGraphMapper.deleteInvalidEdges(placeGroupId);
        int crossGroupEdgeCount = routeGraphMapper.deleteCrossGroupEdges(placeGroupId);
        int selfLoopEdgeCount = routeGraphMapper.deleteSelfLoopEdges(placeGroupId);
        int duplicateEdgeCount = routeGraphMapper.deleteDuplicateDirectedEdges(placeGroupId);
        int totalCount = invalidEdgeCount + crossGroupEdgeCount + selfLoopEdgeCount + duplicateEdgeCount;

        vo.setInvalidEdgeCount(invalidEdgeCount);
        vo.setCrossGroupEdgeCount(crossGroupEdgeCount);
        vo.setSelfLoopEdgeCount(selfLoopEdgeCount);
        vo.setDuplicateEdgeCount(duplicateEdgeCount);
        vo.setTotalCount(totalCount);
        vo.setMessage("已清理 " + totalCount + " 条可自动判定的异常路线边");
        vo.setQuality(quality(placeGroupId));
        return vo;
    }

    public RouteGraphSaveRequest exportGraph(Long placeGroupId) {
        adminService.requireAdmin();
        validatePlaceGroup(placeGroupId);
        return buildSnapshot(placeGroupId);
    }

    @Transactional
    public RouteGraphVersionVO createVersion(Long placeGroupId, RouteGraphVersionCreateRequest request) {
        long adminUserId = adminService.requireAdmin();
        validatePlaceGroup(placeGroupId);
        RouteGraphSaveRequest snapshot = buildSnapshot(placeGroupId);
        RouteGraphVersionRecord version = new RouteGraphVersionRecord();
        version.setPlaceGroupId(placeGroupId);
        version.setVersionNo(routeGraphMapper.nextVersionNo(placeGroupId));
        version.setName(normalizeVersionName(request, version.getVersionNo()));
        version.setRemark(request == null ? null : trimToNull(request.getRemark()));
        version.setNodeCount(snapshot.getNodes().size());
        version.setEdgeCount(snapshot.getEdges().size());
        version.setCreatedBy(adminUserId);
        version.setSnapshotJson(toJson(snapshot));
        routeGraphMapper.insertVersion(version);
        return RouteGraphVersionVO.from(routeGraphMapper.findVersion(placeGroupId, version.getId()));
    }

    @Transactional
    public RouteGraphVO restoreVersion(Long placeGroupId, Long versionId) {
        adminService.requireAdmin();
        validatePlaceGroup(placeGroupId);
        RouteGraphVersionRecord version = routeGraphMapper.findVersion(placeGroupId, versionId);
        if (version == null) {
            throw BusinessException.notFound("路网版本不存在");
        }
        createAutoVersion(placeGroupId, "恢复前自动备份", "恢复版本 v" + version.getVersionNo() + " 前自动创建");
        return save(placeGroupId, fromJson(version.getSnapshotJson()));
    }

    @Transactional
    public RouteGraphVO importGraph(Long placeGroupId, RouteGraphSaveRequest request) {
        adminService.requireAdmin();
        validatePlaceGroup(placeGroupId);
        createAutoVersion(placeGroupId, "导入前自动备份", "导入 JSON 前自动创建");
        return save(placeGroupId, request);
    }

    @Transactional
    public RouteGraphVO save(Long placeGroupId, RouteGraphSaveRequest request) {
        adminService.requireAdmin();
        validatePlaceGroup(placeGroupId);
        if (request == null || request.getNodes() == null) {
            throw BusinessException.badRequest("路网节点不能为空");
        }
        if (request.getEdges() == null) {
            request.setEdges(List.of());
        }
        Long spotId = routeGraphMapper.findSpotIdByPlaceGroupId(placeGroupId);
        if (spotId == null) {
            throw BusinessException.badRequest("当前空间分组未关联景点，无法保存路网");
        }
        Long routeCategoryId = routeGraphMapper.findCategoryIdByCode("scenic");
        if (routeCategoryId == null) {
            throw BusinessException.badRequest("缺少 scenic POI 分类，无法创建路线节点");
        }

        Map<String, RouteGraphNodeRecord> clientNodeMap = new HashMap<>();
        for (RouteGraphNodeRequest node : request.getNodes()) {
            validateNode(node);
            if (isRouteNode(node)) {
                RouteGraphNodeRecord record = buildRouteNode(placeGroupId, spotId, routeCategoryId, node);
                clientNodeMap.put(clientId(node), record);
            } else {
                routeGraphMapper.updatePoiCoordinate(
                        node.getId(),
                        placeGroupId,
                        node.getMapX(),
                        node.getMapY(),
                        longitudeOf(node.getMapX()),
                        latitudeOf(node.getMapY()));
            }
        }

        routeGraphMapper.deleteEdgesByPlaceGroupId(placeGroupId);
        routeGraphMapper.deleteRouteNodesByPlaceGroupId(placeGroupId);

        for (RouteGraphNodeRecord node : clientNodeMap.values()) {
            routeGraphMapper.insertRouteNode(node);
        }

        Map<String, Long> idMap = new HashMap<>();
        for (RouteGraphNodeRequest node : request.getNodes()) {
            String clientId = clientId(node);
            if (isRouteNode(node)) {
                idMap.put(clientId, clientNodeMap.get(clientId).getId());
            } else {
                idMap.put(clientId, node.getId());
            }
        }

        Map<Long, RouteGraphNodeRecord> savedNodes = new HashMap<>();
        for (RouteGraphNodeRecord node : routeGraphMapper.listNodes(placeGroupId)) {
            savedNodes.put(node.getId(), node);
        }
        Set<String> insertedEdges = new HashSet<>();
        for (RouteGraphEdgeRequest edge : request.getEdges()) {
            insertEdgeIfValid(edge, idMap, savedNodes, insertedEdges, false);
            if (edge.getBidirectional() == null || edge.getBidirectional()) {
                insertEdgeIfValid(edge, idMap, savedNodes, insertedEdges, true);
            }
        }
        return graph(placeGroupId);
    }

    @Transactional
    public RouteGraphVO saveWithVersion(Long placeGroupId, RouteGraphSaveWithVersionRequest request) {
        adminService.requireAdmin();
        if (request == null || request.getGraph() == null) {
            throw BusinessException.badRequest("路网保存内容不能为空");
        }
        RouteGraphVO savedGraph = save(placeGroupId, request.getGraph());
        if (Boolean.TRUE.equals(request.getCreateVersion())) {
            RouteGraphVersionCreateRequest versionRequest = new RouteGraphVersionCreateRequest();
            versionRequest.setName(trimToNull(request.getVersionName()));
            versionRequest.setRemark(trimToNull(request.getVersionRemark()));
            createVersion(placeGroupId, versionRequest);
        }
        return savedGraph;
    }

    private RouteGraphVersionRecord findExistingVersion(Long placeGroupId, Long versionId) {
        validatePlaceGroup(placeGroupId);
        if (versionId == null || versionId <= 0) {
            throw BusinessException.badRequest("路网版本参数不合法");
        }
        RouteGraphVersionRecord version = routeGraphMapper.findVersion(placeGroupId, versionId);
        if (version == null) {
            throw BusinessException.notFound("路网版本不存在");
        }
        return version;
    }

    private List<String> buildDiffWarnings(RouteGraphSaveRequest current, RouteGraphSaveRequest target) {
        List<String> warnings = new ArrayList<>();
        if (target.getPlaceGroupId() != null && !target.getPlaceGroupId().equals(current.getPlaceGroupId())) {
            warnings.add("该版本快照记录的 placeGroupId 与当前景点不一致，恢复前需要确认版本来源。");
        }
        if (target.getMap() != null && current.getMap() != null
                && target.getMap().getMapWidth() != null && target.getMap().getMapHeight() != null
                && current.getMap().getMapWidth() != null && current.getMap().getMapHeight() != null
                && (!target.getMap().getMapWidth().equals(current.getMap().getMapWidth())
                || !target.getMap().getMapHeight().equals(current.getMap().getMapHeight()))) {
            warnings.add("版本底图尺寸与当前底图不一致，恢复后点位可能和当前底图错位。");
        }
        return warnings;
    }

    private Map<String, RouteGraphNodeRequest> nodeIdentityMap(List<RouteGraphNodeRequest> nodes) {
        Map<String, RouteGraphNodeRequest> result = new LinkedHashMap<>();
        if (nodes == null) {
            return result;
        }
        for (RouteGraphNodeRequest node : nodes) {
            String identity = nodeIdentity(node);
            if (identity != null) {
                result.putIfAbsent(identity, node);
            }
        }
        return result;
    }

    private String nodeIdentity(RouteGraphNodeRequest node) {
        if (node == null) {
            return null;
        }
        if (isPoiNode(node) && node.getId() != null) {
            return "poi:" + node.getId();
        }
        if (isRouteNode(node)) {
            return "route:"
                    + safeText(node.getName())
                    + ":"
                    + safeInt(node.getMapX())
                    + ":"
                    + safeInt(node.getMapY());
        }
        if (StringUtils.hasText(node.getClientId())) {
            return "client:" + node.getClientId();
        }
        return null;
    }

    private Map<String, RouteGraphDiffEdgeVO> edgeIdentityMap(RouteGraphSaveRequest graph) {
        Map<String, RouteGraphDiffEdgeVO> result = new LinkedHashMap<>();
        Map<String, RouteGraphNodeRequest> byClientId = new HashMap<>();
        for (RouteGraphNodeRequest node : graph.getNodes()) {
            if (StringUtils.hasText(node.getClientId())) {
                byClientId.put(node.getClientId(), node);
            }
        }
        for (RouteGraphEdgeRequest edge : graph.getEdges()) {
            RouteGraphNodeRequest from = byClientId.get(edge.getFromClientId());
            RouteGraphNodeRequest to = byClientId.get(edge.getToClientId());
            String fromIdentity = nodeIdentity(from);
            String toIdentity = nodeIdentity(to);
            if (fromIdentity == null || toIdentity == null || fromIdentity.equals(toIdentity)) {
                continue;
            }
            String key = fromIdentity.compareTo(toIdentity) <= 0
                    ? fromIdentity + "|" + toIdentity
                    : toIdentity + "|" + fromIdentity;
            RouteGraphDiffEdgeVO vo = new RouteGraphDiffEdgeVO();
            vo.setFromName(from == null ? edge.getFromClientId() : from.getName());
            vo.setToName(to == null ? edge.getToClientId() : to.getName());
            vo.setDescription(edge.getDescription());
            if (from != null) {
                vo.setFromMapX(from.getMapX());
                vo.setFromMapY(from.getMapY());
            }
            if (to != null) {
                vo.setToMapX(to.getMapX());
                vo.setToMapY(to.getMapY());
            }
            result.putIfAbsent(key, vo);
        }
        return result;
    }

    private RouteGraphDiffNodeVO nodeDiff(
            RouteGraphNodeRequest displayNode,
            RouteGraphNodeRequest currentNode,
            RouteGraphNodeRequest versionNode,
            String changeType) {
        RouteGraphDiffNodeVO vo = new RouteGraphDiffNodeVO();
        vo.setName(displayNode.getName());
        vo.setNodeType(isRouteNode(displayNode) ? "route" : "poi");
        vo.setChangeType(changeType);
        if (currentNode != null) {
            vo.setCurrentMapX(currentNode.getMapX());
            vo.setCurrentMapY(currentNode.getMapY());
        }
        if (versionNode != null) {
            vo.setVersionMapX(versionNode.getMapX());
            vo.setVersionMapY(versionNode.getMapY());
        }
        return vo;
    }

    private boolean coordinatesChanged(RouteGraphNodeRequest currentNode, RouteGraphNodeRequest targetNode) {
        return !safeInt(currentNode.getMapX()).equals(safeInt(targetNode.getMapX()))
                || !safeInt(currentNode.getMapY()).equals(safeInt(targetNode.getMapY()));
    }

    private boolean isPoiNode(RouteGraphNodeRequest node) {
        return node != null && !isRouteNode(node);
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private Integer safeInt(Integer value) {
        return value == null ? -1 : value;
    }

    private void createAutoVersion(Long placeGroupId, String name, String remark) {
        RouteGraphVersionCreateRequest request = new RouteGraphVersionCreateRequest();
        request.setName(name);
        request.setRemark(remark);
        createVersion(placeGroupId, request);
    }

    private RouteGraphSaveRequest buildSnapshot(Long placeGroupId) {
        RouteGraphSaveRequest snapshot = new RouteGraphSaveRequest();
        List<RouteGraphNodeRequest> nodes = routeGraphMapper.listNodes(placeGroupId).stream()
                .map(this::toNodeRequest)
                .toList();
        List<RouteGraphEdgeRequest> edges = collapseEdges(routeGraphMapper.listEdges(placeGroupId));
        fillExportMetadata(snapshot, placeGroupId, nodes, edges);
        snapshot.setNodes(nodes);
        snapshot.setEdges(edges);
        return snapshot;
    }

    private void fillExportMetadata(
            RouteGraphSaveRequest snapshot,
            Long placeGroupId,
            List<RouteGraphNodeRequest> nodes,
            List<RouteGraphEdgeRequest> edges) {
        snapshot.setSchemaVersion("tourwise-route-graph-v1");
        snapshot.setExportedAt(LocalDateTime.now().toString());
        snapshot.setPlaceGroupId(placeGroupId);
        Map<String, Object> summary = routeGraphMapper.findPlaceGroupSummary(placeGroupId);
        if (summary != null) {
            Map<String, Object> row = MapUtil.normalize(summary);
            snapshot.setPlaceGroupName(VoConvert.string(row, "placeGroupName"));
            snapshot.setPlaceGroupShortName(VoConvert.string(row, "placeGroupShortName"));
            snapshot.setRouteGraphStatus(VoConvert.string(row, "routeGraphStatus"));
            snapshot.setCity(VoConvert.string(row, "city"));
            snapshot.setAddress(VoConvert.string(row, "address"));
        }
        snapshot.setMap(RouteMapVO.from(routeMapMapper.findByPlaceGroupId(placeGroupId)));

        RouteGraphExportStatsVO stats = new RouteGraphExportStatsVO();
        stats.setPoiCount((int) nodes.stream().filter(node -> !isRouteNode(node)).count());
        stats.setRouteNodeCount((int) nodes.stream().filter(this::isRouteNode).count());
        stats.setEdgeCount(edges.size());
        snapshot.setStats(stats);
    }

    private RouteGraphNodeRequest toNodeRequest(RouteGraphNodeRecord record) {
        RouteGraphNodeRequest request = new RouteGraphNodeRequest();
        request.setId(record.getId());
        request.setClientId("id:" + record.getId());
        request.setName(record.getName());
        request.setNodeType("route".equals(record.getAreaCode()) ? "route" : "poi");
        request.setCategoryCode(record.getCategoryCode());
        request.setAreaCode(record.getAreaCode());
        request.setAreaName(record.getAreaName());
        request.setMapX(record.getMapX());
        request.setMapY(record.getMapY());
        request.setVisible(!"route".equals(record.getAreaCode()));
        return request;
    }

    private RouteGraphIssueVO nodeIssue(
            Map<String, Object> rawRow,
            String type,
            String severity,
            String title,
            String description) {
        Map<String, Object> row = MapUtil.normalize(rawRow);
        RouteGraphIssueVO issue = baseIssue(type, severity, title, description);
        issue.setNodeId(VoConvert.longValue(row, "nodeId"));
        issue.setNodeName(VoConvert.string(row, "nodeName"));
        return issue;
    }

    private RouteGraphIssueVO edgeIssue(
            Map<String, Object> rawRow,
            String type,
            String severity,
            String title,
            String description) {
        Map<String, Object> row = MapUtil.normalize(rawRow);
        RouteGraphIssueVO issue = baseIssue(type, severity, title, description);
        issue.setEdgeId(VoConvert.longValue(row, "edgeId"));
        issue.setFromNodeId(VoConvert.longValue(row, "fromNodeId"));
        issue.setFromNodeName(VoConvert.string(row, "fromNodeName"));
        issue.setToNodeId(VoConvert.longValue(row, "toNodeId"));
        issue.setToNodeName(VoConvert.string(row, "toNodeName"));
        issue.setDistanceM(VoConvert.intValue(row, "distanceM"));
        return issue;
    }

    private RouteGraphIssueVO baseIssue(String type, String severity, String title, String description) {
        RouteGraphIssueVO issue = new RouteGraphIssueVO();
        issue.setType(type);
        issue.setSeverity(severity);
        issue.setTitle(title);
        issue.setDescription(description);
        return issue;
    }

    private Integer intValue(Map<String, Object> row, String key) {
        Integer value = VoConvert.intValue(row, key);
        return value == null ? 0 : value;
    }

    private List<RouteGraphEdgeRequest> collapseEdges(List<RouteGraphEdgeRecord> rawEdges) {
        Map<String, RouteGraphEdgeRequest> result = new java.util.LinkedHashMap<>();
        for (RouteGraphEdgeRecord edge : rawEdges) {
            Long a = edge.getFromPoiId();
            Long b = edge.getToPoiId();
            if (a == null || b == null || a.equals(b)) {
                continue;
            }
            Long from = Math.min(a, b);
            Long to = Math.max(a, b);
            String key = from + "|" + to;
            if (result.containsKey(key)) {
                continue;
            }
            RouteGraphEdgeRequest request = new RouteGraphEdgeRequest();
            request.setFromClientId("id:" + from);
            request.setToClientId("id:" + to);
            request.setDescription(cleanupDescription(edge.getDescription()));
            request.setTransportType(StringUtils.hasText(edge.getTransportType()) ? edge.getTransportType() : "walk");
            request.setIndoor(Boolean.TRUE.equals(edge.getIndoor()));
            request.setBidirectional(true);
            result.put(key, request);
        }
        return List.copyOf(result.values());
    }

    private String cleanupDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return "";
        }
        return description.replaceFirst("^返回：", "").trim();
    }

    private String normalizeVersionName(RouteGraphVersionCreateRequest request, Integer versionNo) {
        String name = request == null ? null : trimToNull(request.getName());
        return name == null ? "路网版本 v" + versionNo : name;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String toJson(RouteGraphSaveRequest snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("路网版本序列化失败");
        }
    }

    private RouteGraphSaveRequest fromJson(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson, RouteGraphSaveRequest.class);
        } catch (JsonProcessingException e) {
            throw BusinessException.badRequest("路网版本内容解析失败");
        }
    }

    private void insertEdgeIfValid(
            RouteGraphEdgeRequest edge,
            Map<String, Long> idMap,
            Map<Long, RouteGraphNodeRecord> savedNodes,
            Set<String> insertedEdges,
            boolean reverse) {
        Long fromId = idMap.get(reverse ? edge.getToClientId() : edge.getFromClientId());
        Long toId = idMap.get(reverse ? edge.getFromClientId() : edge.getToClientId());
        if (fromId == null || toId == null || fromId.equals(toId)) {
            return;
        }
        String key = fromId + "->" + toId;
        if (!insertedEdges.add(key)) {
            return;
        }
        RouteGraphNodeRecord from = savedNodes.get(fromId);
        RouteGraphNodeRecord to = savedNodes.get(toId);
        if (from == null || to == null) {
            return;
        }
        int distance = distanceMeters(from, to);
        RouteEdge routeEdge = new RouteEdge();
        routeEdge.setFromPoiId(fromId);
        routeEdge.setToPoiId(toId);
        routeEdge.setDistanceM(distance);
        routeEdge.setDurationMin(Math.max(1, (int) Math.ceil(distance / 80.0)));
        routeEdge.setCongestionFactor(BigDecimal.ONE);
        routeEdge.setIndoor(Boolean.TRUE.equals(edge.getIndoor()));
        routeEdge.setTransportType(StringUtils.hasText(edge.getTransportType()) ? edge.getTransportType() : "walk");
        String description = StringUtils.hasText(edge.getDescription()) ? edge.getDescription().trim() : "图上标定路段";
        routeEdge.setDescription(reverse ? "返回：" + description : description);
        routeGraphMapper.insertEdge(routeEdge);
    }

    private RouteGraphNodeRecord buildRouteNode(
            Long placeGroupId,
            Long spotId,
            Long routeCategoryId,
            RouteGraphNodeRequest node) {
        RouteGraphNodeRecord record = new RouteGraphNodeRecord();
        record.setSpotId(spotId);
        record.setPlaceGroupId(placeGroupId);
        record.setCategoryId(routeCategoryId);
        record.setName(StringUtils.hasText(node.getName()) ? node.getName().trim() : "路口节点");
        record.setAreaCode("route");
        record.setAreaName("路线节点");
        record.setMapX(node.getMapX());
        record.setMapY(node.getMapY());
        record.setLongitude(longitudeOf(node.getMapX()));
        record.setLatitude(latitudeOf(node.getMapY()));
        return record;
    }

    private void validateNode(RouteGraphNodeRequest node) {
        if (node == null || !StringUtils.hasText(node.getClientId())) {
            throw BusinessException.badRequest("路网节点缺少 clientId");
        }
        if (!isRouteNode(node) && (node.getId() == null || node.getId() <= 0)) {
            throw BusinessException.badRequest("已有 POI 节点缺少数据库 ID");
        }
        if (node.getMapX() == null || node.getMapY() == null) {
            throw BusinessException.badRequest("路网节点缺少平面图坐标");
        }
    }

    private void validatePlaceGroup(Long placeGroupId) {
        if (placeGroupId == null || placeGroupId <= 0) {
            throw BusinessException.badRequest("景点分组参数不合法");
        }
        if (routeGraphMapper.existsPlaceGroup(placeGroupId) <= 0) {
            throw BusinessException.notFound("景点分组不存在");
        }
    }

    private boolean isRouteNode(RouteGraphNodeRequest node) {
        return "route".equals(node.getNodeType()) || "route".equals(node.getAreaCode());
    }

    private String clientId(RouteGraphNodeRequest node) {
        return node.getClientId();
    }

    private int distanceMeters(RouteGraphNodeRecord from, RouteGraphNodeRecord to) {
        int dx = to.getMapX() - from.getMapX();
        int dy = to.getMapY() - from.getMapY();
        return Math.max(1, (int) Math.round(Math.sqrt(dx * dx + dy * dy) * MAP_PIXEL_TO_METER));
    }

    private BigDecimal longitudeOf(Integer mapX) {
        return BASE_LONGITUDE.add(BigDecimal.valueOf(mapX - 1024L).multiply(LONGITUDE_PER_PIXEL))
                .setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal latitudeOf(Integer mapY) {
        return BASE_LATITUDE.subtract(BigDecimal.valueOf(mapY - 990L).multiply(LATITUDE_PER_PIXEL))
                .setScale(6, RoundingMode.HALF_UP);
    }
}
