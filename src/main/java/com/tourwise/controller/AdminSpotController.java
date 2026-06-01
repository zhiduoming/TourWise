package com.tourwise.controller;

import com.tourwise.common.ApiResponse;
import com.tourwise.common.PageResult;
import com.tourwise.dto.AdminPoiRequest;
import com.tourwise.dto.AdminSpotCreateRequest;
import com.tourwise.dto.AdminSpotTagsRequest;
import com.tourwise.dto.AdminSpotUpdateRequest;
import com.tourwise.service.AdminSpotService;
import com.tourwise.vo.admin.AdminPoiCategoryVO;
import com.tourwise.vo.admin.AdminPoiVO;
import com.tourwise.vo.admin.AdminSpotVO;
import com.tourwise.vo.admin.AdminTagVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/admin")
public class AdminSpotController {
    private final AdminSpotService adminSpotService;

    public AdminSpotController(AdminSpotService adminSpotService) {
        this.adminSpotService = adminSpotService;
    }

    @GetMapping("/spots")
    public ApiResponse<PageResult<AdminSpotVO>> spots(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String routeGraphStatus,
            @RequestParam(required = false) String qualityIssue,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(adminSpotService.spots(keyword, status, routeGraphStatus, qualityIssue, page, pageSize));
    }

    @GetMapping("/spots/{id}")
    public ApiResponse<AdminSpotVO> spot(@PathVariable Long id) {
        return ApiResponse.ok(adminSpotService.spot(id));
    }

    @PostMapping("/spots")
    public ApiResponse<AdminSpotVO> createSpot(@RequestBody AdminSpotCreateRequest request) {
        return ApiResponse.ok(adminSpotService.createSpot(request));
    }

    @PutMapping("/spots/{id}")
    public ApiResponse<AdminSpotVO> updateSpot(
            @PathVariable Long id,
            @RequestBody AdminSpotUpdateRequest request) {
        return ApiResponse.ok(adminSpotService.updateSpot(id, request));
    }

    @PatchMapping("/spots/{id}/status")
    public ApiResponse<AdminSpotVO> updateSpotStatus(
            @PathVariable Long id,
            @RequestParam @Min(0) @Max(1) Integer status) {
        return ApiResponse.ok(adminSpotService.updateSpotStatus(id, status));
    }

    @GetMapping("/spots/{spotId}/pois")
    public ApiResponse<PageResult<AdminPoiVO>> pois(
            @PathVariable Long spotId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String qualityIssue,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int pageSize) {
        return ApiResponse.ok(adminSpotService.pois(spotId, keyword, status, qualityIssue, page, pageSize));
    }

    @PostMapping("/spots/{spotId}/pois")
    public ApiResponse<AdminPoiVO> createPoi(
            @PathVariable Long spotId,
            @RequestBody AdminPoiRequest request) {
        return ApiResponse.ok(adminSpotService.createPoi(spotId, request));
    }

    @PutMapping("/pois/{poiId}")
    public ApiResponse<AdminPoiVO> updatePoi(
            @PathVariable Long poiId,
            @RequestBody AdminPoiRequest request) {
        return ApiResponse.ok(adminSpotService.updatePoi(poiId, request));
    }

    @PatchMapping("/pois/{poiId}/status")
    public ApiResponse<AdminPoiVO> updatePoiStatus(
            @PathVariable Long poiId,
            @RequestParam @Min(0) @Max(1) Integer status) {
        return ApiResponse.ok(adminSpotService.updatePoiStatus(poiId, status));
    }

    @GetMapping("/poi-categories")
    public ApiResponse<List<AdminPoiCategoryVO>> categories() {
        return ApiResponse.ok(adminSpotService.categories());
    }

    @GetMapping("/tags")
    public ApiResponse<List<AdminTagVO>> tags(@RequestParam(required = false) String tagType) {
        return ApiResponse.ok(adminSpotService.tags(tagType));
    }

    @GetMapping("/spots/{spotId}/tags")
    public ApiResponse<List<AdminTagVO>> spotTags(@PathVariable Long spotId) {
        return ApiResponse.ok(adminSpotService.spotTags(spotId));
    }

    @PutMapping("/spots/{spotId}/tags")
    public ApiResponse<List<AdminTagVO>> updateSpotTags(
            @PathVariable Long spotId,
            @RequestBody(required = false) AdminSpotTagsRequest request) {
        return ApiResponse.ok(adminSpotService.updateSpotTags(spotId, request));
    }
}
