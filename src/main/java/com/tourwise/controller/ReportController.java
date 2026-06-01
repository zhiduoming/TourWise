package com.tourwise.controller;

import com.tourwise.common.ApiResponse;
import com.tourwise.dto.ReportCreateRequest;
import com.tourwise.service.ContentReportService;
import com.tourwise.vo.common.ActionResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportController {
    private final ContentReportService contentReportService;

    public ReportController(ContentReportService contentReportService) {
        this.contentReportService = contentReportService;
    }

    @PostMapping
    public ApiResponse<ActionResultVO> create(@RequestBody @Valid ReportCreateRequest request) {
        return ApiResponse.ok(contentReportService.create(request));
    }
}
