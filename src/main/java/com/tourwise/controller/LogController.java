package com.tourwise.controller;

import com.tourwise.dto.*;
import com.tourwise.service.*;

import com.tourwise.common.ApiResponse;
import com.tourwise.common.PageResult;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.log.LogVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/log")
public class LogController {
    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<LogVO>> list(
            @RequestParam(required = false) Long circleId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long spotId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "all") String tab,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(logService.list(circleId, userId, spotId, keyword, tab, page, pageSize));
    }

    @GetMapping("/my")
    public ApiResponse<PageResult<LogVO>> myLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "all") String tab,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(logService.myLogs(keyword, tab, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<LogVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(logService.detail(id));
    }

    @PostMapping("/create")
    public ApiResponse<ActionResultVO> create(@RequestBody @Valid LogCreateRequest request) {
        return ApiResponse.ok(logService.create(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<ActionResultVO> delete(@PathVariable Long id) {
        return ApiResponse.ok(logService.delete(id));
    }

    @PostMapping("/{id}/like")
    public ApiResponse<ActionResultVO> like(@PathVariable Long id) {
        return ApiResponse.ok(logService.toggleLike(id));
    }
}
