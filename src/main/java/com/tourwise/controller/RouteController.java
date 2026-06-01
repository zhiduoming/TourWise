package com.tourwise.controller;

import com.tourwise.dto.*;
import com.tourwise.service.*;

import com.tourwise.common.ApiResponse;
import com.tourwise.common.PageResult;
import com.tourwise.vo.route.RouteMapVO;
import com.tourwise.vo.route.RoutePoiVO;
import com.tourwise.vo.route.RouteRecordVO;
import com.tourwise.vo.route.RouteResultVO;
import com.tourwise.vo.route.RouteScopeVO;
import com.tourwise.vo.route.RouteSpotVO;
import com.tourwise.vo.route.AmapConfigVO;
import com.tourwise.vo.route.LocationResolveVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/route")
public class RouteController {
    private final RouteService routeService;
    private final RouteMapService routeMapService;
    private final AmapRouteService amapRouteService;
    private final RouteLocationService routeLocationService;
    private final RouteRecordService routeRecordService;

    public RouteController(
            RouteService routeService,
            RouteMapService routeMapService,
            AmapRouteService amapRouteService,
            RouteLocationService routeLocationService,
            RouteRecordService routeRecordService) {
        this.routeService = routeService;
        this.routeMapService = routeMapService;
        this.amapRouteService = amapRouteService;
        this.routeLocationService = routeLocationService;
        this.routeRecordService = routeRecordService;
    }

    @GetMapping("/spots")
    public ApiResponse<List<RouteSpotVO>> spots() {
        return ApiResponse.ok(routeService.spots());
    }

    @GetMapping("/scopes")
    public ApiResponse<List<RouteScopeVO>> scopes() {
        return ApiResponse.ok(routeService.scopes());
    }

    @GetMapping("/pois")
    public ApiResponse<List<RoutePoiVO>> pois(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long spotId,
            @RequestParam(required = false) Long placeGroupId) {
        return ApiResponse.ok(routeService.pois(keyword, category, spotId, placeGroupId));
    }

    @GetMapping("/maps/{placeGroupId}")
    public ApiResponse<RouteMapVO> map(@PathVariable Long placeGroupId) {
        return ApiResponse.ok(routeMapService.getMap(placeGroupId));
    }

    @GetMapping("/amap/config")
    public ApiResponse<AmapConfigVO> amapConfig() {
        return ApiResponse.ok(amapRouteService.config());
    }

    @PostMapping("/amap/plan")
    public ApiResponse<RouteResultVO> amapPlan(@RequestBody @Valid AmapRouteRequest request) {
        return ApiResponse.ok(amapRouteService.plan(request));
    }

    @PostMapping("/location/resolve")
    public ApiResponse<LocationResolveVO> resolveLocation(@RequestBody @Valid LocationResolveRequest request) {
        return ApiResponse.ok(routeLocationService.resolve(request));
    }

    @PostMapping("/shortest")
    public ApiResponse<RouteResultVO> shortest(@RequestBody @Valid RouteRequest request) {
        return ApiResponse.ok(routeService.shortest(request, false));
    }

    @PostMapping("/optimal")
    public ApiResponse<RouteResultVO> optimal(@RequestBody @Valid OptimalRouteRequest request) {
        return ApiResponse.ok(routeService.optimal(request));
    }

    @PostMapping("/indoor")
    public ApiResponse<RouteResultVO> indoor(@RequestBody @Valid RouteRequest request) {
        return ApiResponse.ok(routeService.shortest(request, true));
    }

    @PostMapping("/records")
    public ApiResponse<RouteRecordVO> saveRecord(@RequestBody RouteRecordSaveRequest request) {
        return ApiResponse.ok(routeRecordService.save(request));
    }

    @GetMapping("/records")
    public ApiResponse<PageResult<RouteRecordVO>> records(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(routeRecordService.list(page, pageSize));
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        routeRecordService.delete(id);
        return ApiResponse.ok(null);
    }
}
