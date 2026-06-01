package com.tourwise.controller;

import com.tourwise.common.ApiResponse;
import com.tourwise.dto.RouteGraphPoiCreateRequest;
import com.tourwise.dto.RouteGraphSaveRequest;
import com.tourwise.dto.RouteGraphSaveWithVersionRequest;
import com.tourwise.dto.RouteGraphVersionCreateRequest;
import com.tourwise.service.AdminRouteGraphService;
import com.tourwise.vo.route.RouteGraphCleanupVO;
import com.tourwise.vo.route.RouteGraphDiffVO;
import com.tourwise.vo.route.RouteGraphInspectionVO;
import com.tourwise.vo.route.RouteGraphNodeVO;
import com.tourwise.vo.route.RouteGraphVO;
import com.tourwise.vo.route.RouteGraphVersionVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/route-graphs")
public class AdminRouteGraphController {
    private final AdminRouteGraphService routeGraphService;

    public AdminRouteGraphController(AdminRouteGraphService routeGraphService) {
        this.routeGraphService = routeGraphService;
    }

    @GetMapping("/{placeGroupId}")
    public ApiResponse<RouteGraphVO> graph(@PathVariable Long placeGroupId) {
        return ApiResponse.ok(routeGraphService.graph(placeGroupId));
    }

    @GetMapping("/{placeGroupId}/export")
    public ApiResponse<RouteGraphSaveRequest> exportGraph(@PathVariable Long placeGroupId) {
        return ApiResponse.ok(routeGraphService.exportGraph(placeGroupId));
    }

    @GetMapping("/{placeGroupId}/versions")
    public ApiResponse<List<RouteGraphVersionVO>> versions(@PathVariable Long placeGroupId) {
        return ApiResponse.ok(routeGraphService.versions(placeGroupId));
    }

    @GetMapping("/{placeGroupId}/quality")
    public ApiResponse<RouteGraphInspectionVO> quality(@PathVariable Long placeGroupId) {
        return ApiResponse.ok(routeGraphService.quality(placeGroupId));
    }

    @PostMapping("/{placeGroupId}/quality/cleanup")
    public ApiResponse<RouteGraphCleanupVO> cleanupQualityIssues(@PathVariable Long placeGroupId) {
        return ApiResponse.ok(routeGraphService.cleanupQualityIssues(placeGroupId));
    }

    @PostMapping("/{placeGroupId}/pois")
    public ApiResponse<RouteGraphNodeVO> createPoi(
            @PathVariable Long placeGroupId,
            @RequestBody RouteGraphPoiCreateRequest request) {
        return ApiResponse.ok(routeGraphService.createPoi(placeGroupId, request));
    }

    @DeleteMapping("/{placeGroupId}/pois/{poiId}")
    public ApiResponse<Void> deletePoi(
            @PathVariable Long placeGroupId,
            @PathVariable Long poiId) {
        routeGraphService.deletePoi(placeGroupId, poiId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{placeGroupId}/versions")
    public ApiResponse<RouteGraphVersionVO> createVersion(
            @PathVariable Long placeGroupId,
            @RequestBody(required = false) RouteGraphVersionCreateRequest request) {
        return ApiResponse.ok(routeGraphService.createVersion(placeGroupId, request));
    }

    @GetMapping("/{placeGroupId}/versions/{versionId}/snapshot")
    public ApiResponse<RouteGraphSaveRequest> versionSnapshot(
            @PathVariable Long placeGroupId,
            @PathVariable Long versionId) {
        return ApiResponse.ok(routeGraphService.versionSnapshot(placeGroupId, versionId));
    }

    @GetMapping("/{placeGroupId}/versions/{versionId}/diff")
    public ApiResponse<RouteGraphDiffVO> diffVersion(
            @PathVariable Long placeGroupId,
            @PathVariable Long versionId) {
        return ApiResponse.ok(routeGraphService.diffVersion(placeGroupId, versionId));
    }

    @PostMapping("/{placeGroupId}/versions/{versionId}/restore")
    public ApiResponse<RouteGraphVO> restoreVersion(
            @PathVariable Long placeGroupId,
            @PathVariable Long versionId) {
        return ApiResponse.ok(routeGraphService.restoreVersion(placeGroupId, versionId));
    }

    @PostMapping("/{placeGroupId}/import")
    public ApiResponse<RouteGraphVO> importGraph(
            @PathVariable Long placeGroupId,
            @RequestBody RouteGraphSaveRequest request) {
        return ApiResponse.ok(routeGraphService.importGraph(placeGroupId, request));
    }

    @PutMapping("/{placeGroupId}")
    public ApiResponse<RouteGraphVO> save(
            @PathVariable Long placeGroupId,
            @RequestBody RouteGraphSaveRequest request) {
        return ApiResponse.ok(routeGraphService.save(placeGroupId, request));
    }

    @PutMapping("/{placeGroupId}/with-version")
    public ApiResponse<RouteGraphVO> saveWithVersion(
            @PathVariable Long placeGroupId,
            @RequestBody RouteGraphSaveWithVersionRequest request) {
        return ApiResponse.ok(routeGraphService.saveWithVersion(placeGroupId, request));
    }
}
