package com.tourwise.controller;

import com.tourwise.dto.*;
import com.tourwise.service.*;

import com.tourwise.common.ApiResponse;
import com.tourwise.common.PageResult;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.recommend.RecommendVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/recommend")
public class RecommendController {
    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/hot-top10")
    public ApiResponse<List<RecommendVO>> hotTop10() {
        return ApiResponse.ok(recommendService.hotTop10());
    }

    @GetMapping("/list")
    public ApiResponse<PageResult<RecommendVO>> list(
            @RequestParam(required = false) String strategy,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String distance,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean routeRequired,
            @RequestParam(required = false) Boolean foodRequired,
            @RequestParam(required = false) Boolean avoidVisited,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(recommendService.list(
                strategy,
                scene,
                type,
                category,
                distance,
                purpose,
                city,
                Boolean.TRUE.equals(routeRequired),
                Boolean.TRUE.equals(foodRequired),
                Boolean.TRUE.equals(avoidVisited),
                page,
                pageSize));
    }

    @PostMapping("/rating")
    public ApiResponse<ActionResultVO> rate(@RequestBody @Valid RatingRequest request) {
        return ApiResponse.ok(recommendService.rate(request));
    }
}
