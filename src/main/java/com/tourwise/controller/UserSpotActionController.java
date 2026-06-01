package com.tourwise.controller;

import com.tourwise.common.ApiResponse;
import com.tourwise.common.PageResult;
import com.tourwise.dto.SpotActionRequest;
import com.tourwise.service.UserSpotActionService;
import com.tourwise.vo.search.FacilityVO;
import com.tourwise.vo.user.SpotActionStateVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/user/spot-actions")
public class UserSpotActionController {
    private final UserSpotActionService userSpotActionService;

    public UserSpotActionController(UserSpotActionService userSpotActionService) {
        this.userSpotActionService = userSpotActionService;
    }

    @GetMapping("/{targetId}")
    public ApiResponse<SpotActionStateVO> state(
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "poi") String targetType) {
        return ApiResponse.ok(userSpotActionService.state(targetType, targetId));
    }

    @PostMapping("/browse")
    public ApiResponse<SpotActionStateVO> browse(@RequestBody @Valid SpotActionRequest request) {
        return ApiResponse.ok(userSpotActionService.recordBrowse(request));
    }

    @PostMapping("/favorite")
    public ApiResponse<SpotActionStateVO> favorite(@RequestBody @Valid SpotActionRequest request) {
        return ApiResponse.ok(userSpotActionService.toggleFavorite(request));
    }

    @PostMapping("/want")
    public ApiResponse<SpotActionStateVO> want(@RequestBody @Valid SpotActionRequest request) {
        return ApiResponse.ok(userSpotActionService.toggleWant(request));
    }

    @PostMapping("/visited")
    public ApiResponse<SpotActionStateVO> visited(@RequestBody @Valid SpotActionRequest request) {
        return ApiResponse.ok(userSpotActionService.toggleVisited(request));
    }

    @PostMapping("/dislike")
    public ApiResponse<SpotActionStateVO> dislike(@RequestBody @Valid SpotActionRequest request) {
        return ApiResponse.ok(userSpotActionService.dislike(request));
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<FacilityVO>> list(
            @RequestParam(defaultValue = "favorite") String type,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(userSpotActionService.list(type, page, pageSize));
    }
}
