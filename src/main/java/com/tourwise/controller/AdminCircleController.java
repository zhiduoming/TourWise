package com.tourwise.controller;

import com.tourwise.common.ApiResponse;
import com.tourwise.common.PageResult;
import com.tourwise.dto.AdminCircleUpdateRequest;
import com.tourwise.service.CircleService;
import com.tourwise.vo.circle.CircleVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/circles")
public class AdminCircleController {
    private final CircleService circleService;

    public AdminCircleController(CircleService circleService) {
        this.circleService = circleService;
    }

    @GetMapping
    public ApiResponse<PageResult<CircleVO>> circles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(circleService.adminList(keyword, status, page, pageSize));
    }

    @PutMapping("/{id}")
    public ApiResponse<CircleVO> updateCircle(
            @PathVariable Long id,
            @RequestBody @Valid AdminCircleUpdateRequest request) {
        return ApiResponse.ok(circleService.adminUpdate(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<CircleVO> updateCircleStatus(
            @PathVariable Long id,
            @RequestParam int status) {
        return ApiResponse.ok(circleService.adminUpdateStatus(id, status));
    }
}
