package com.tourwise.controller;

import com.tourwise.common.ApiResponse;
import com.tourwise.service.RouteMapService;
import com.tourwise.vo.route.RouteMapVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/maps")
public class AdminMapController {
    private final RouteMapService routeMapService;

    public AdminMapController(RouteMapService routeMapService) {
        this.routeMapService = routeMapService;
    }

    @PostMapping("/{placeGroupId}")
    public ApiResponse<RouteMapVO> upload(
            @PathVariable Long placeGroupId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(routeMapService.uploadMap(placeGroupId, file));
    }
}
