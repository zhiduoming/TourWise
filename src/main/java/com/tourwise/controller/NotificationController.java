package com.tourwise.controller;

import com.tourwise.common.ApiResponse;
import com.tourwise.common.PageResult;
import com.tourwise.service.NotificationService;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.user.NotificationVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResult<NotificationVO>> list(
            @RequestParam(required = false) Boolean onlyUnread,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int pageSize) {
        return ApiResponse.ok(notificationService.list(onlyUnread, page, pageSize));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Integer>> unreadCount() {
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<ActionResultVO> markRead(@PathVariable Long id) {
        return ApiResponse.ok(notificationService.markRead(id));
    }

    @PatchMapping("/read-all")
    public ApiResponse<ActionResultVO> markAllRead() {
        return ApiResponse.ok(notificationService.markAllRead());
    }
}
