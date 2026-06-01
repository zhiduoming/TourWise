package com.tourwise.controller;

import com.tourwise.common.ApiResponse;
import com.tourwise.service.AdminDashboardService;
import com.tourwise.vo.admin.AdminDashboardVO;
import com.tourwise.vo.admin.RouteGraphQualityVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardVO> dashboard() {
        return ApiResponse.ok(adminDashboardService.dashboard());
    }

    @GetMapping("/route-quality")
    public ApiResponse<List<RouteGraphQualityVO>> routeQuality() {
        return ApiResponse.ok(adminDashboardService.routeQuality());
    }
}
