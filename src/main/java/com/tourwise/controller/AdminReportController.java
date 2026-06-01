package com.tourwise.controller;

import com.tourwise.common.ApiResponse;
import com.tourwise.common.PageResult;
import com.tourwise.dto.AdminReportHandleRequest;
import com.tourwise.service.ContentReportService;
import com.tourwise.vo.admin.ContentReportVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/reports")
public class AdminReportController {
    private final ContentReportService contentReportService;

    public AdminReportController(ContentReportService contentReportService) {
        this.contentReportService = contentReportService;
    }

    @GetMapping
    public ApiResponse<PageResult<ContentReportVO>> list(
            @RequestParam(required = false, defaultValue = "pending") String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int pageSize) {
        return ApiResponse.ok(contentReportService.list(status, targetType, keyword, page, pageSize));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ContentReportVO> handle(
            @PathVariable Long id,
            @RequestBody @Valid AdminReportHandleRequest request) {
        return ApiResponse.ok(contentReportService.handle(id, request));
    }
}
