package com.tourwise.controller;

import com.tourwise.dto.*;
import com.tourwise.service.*;

import com.tourwise.common.ApiResponse;
import com.tourwise.common.PageResult;
import com.tourwise.vo.circle.CircleListVO;
import com.tourwise.vo.circle.CircleVO;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.log.CommentVO;
import com.tourwise.vo.log.LogVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/circle")
public class CircleController {
    private final CircleService circleService;
    private final LogService logService;

    public CircleController(CircleService circleService, LogService logService) {
        this.circleService = circleService;
        this.logService = logService;
    }

    @GetMapping("/list")
    public ApiResponse<CircleListVO> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(circleService.list(keyword));
    }

    @PostMapping("/create")
    public ApiResponse<ActionResultVO> create(@RequestBody @Valid CircleCreateRequest request) {
        return ApiResponse.ok(circleService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<CircleVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(circleService.detail(id));
    }

    @PostMapping("/{id}/join")
    public ApiResponse<ActionResultVO> join(@PathVariable Long id) {
        return ApiResponse.ok(circleService.join(id));
    }

    @PostMapping("/{id}/leave")
    public ApiResponse<ActionResultVO> leave(@PathVariable Long id) {
        return ApiResponse.ok(circleService.leave(id));
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<PageResult<LogVO>> logs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        circleService.ensureExists(id);
        return ApiResponse.ok(logService.list(id, null, null, null, "all", page, pageSize));
    }

    @PostMapping("/{id}/logs")
    public ApiResponse<ActionResultVO> createLog(@PathVariable Long id, @RequestBody @Valid LogCreateRequest request) {
        circleService.ensureMember(id);
        return ApiResponse.ok(logService.createForCircle(request, id));
    }

    @PostMapping("/{logId}/like")
    public ApiResponse<ActionResultVO> like(@PathVariable Long logId) {
        return ApiResponse.ok(logService.toggleLike(logId));
    }

    @GetMapping("/{logId}/comments")
    public ApiResponse<PageResult<CommentVO>> comments(@PathVariable Long logId) {
        return ApiResponse.ok(logService.comments(logId));
    }

    @PostMapping("/{logId}/comments")
    public ApiResponse<ActionResultVO> createComment(@PathVariable Long logId, @RequestBody @Valid CommentRequest request) {
        return ApiResponse.ok(logService.createComment(logId, request));
    }
}
