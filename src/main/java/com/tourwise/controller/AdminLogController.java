package com.tourwise.controller;

import com.tourwise.common.ApiResponse;
import com.tourwise.common.PageResult;
import com.tourwise.service.LogService;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.log.CommentVO;
import com.tourwise.vo.log.LogVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/logs")
public class AdminLogController {
    private final LogService logService;

    public AdminLogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public ApiResponse<PageResult<LogVO>> logs(
            @RequestParam(required = false) Long circleId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long spotId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "all") String tab,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(logService.adminLogs(circleId, userId, spotId, keyword, tab, page, pageSize));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<ActionResultVO> deleteLog(@PathVariable Long id) {
        return ApiResponse.ok(logService.adminDelete(id));
    }

    @GetMapping("/{logId}/comments")
    public ApiResponse<PageResult<CommentVO>> comments(@PathVariable Long logId) {
        return ApiResponse.ok(logService.adminComments(logId));
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<ActionResultVO> deleteComment(@PathVariable Long commentId) {
        return ApiResponse.ok(logService.adminDeleteComment(commentId));
    }
}
