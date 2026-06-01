package com.tourwise.controller;

import com.tourwise.dto.*;
import com.tourwise.service.*;

import com.tourwise.common.ApiResponse;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.food.FoodVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/food")
public class FoodController {
    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping("/list")
    public ApiResponse<List<FoodVO>> list(
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String price,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Long near,
            @RequestParam(required = false) Long spotId,
            @RequestParam(required = false) Long placeGroupId,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(foodService.list(cuisine, price, sort, near, spotId, placeGroupId, limit));
    }

    @GetMapping("/list/{id}")
    public ApiResponse<FoodVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(foodService.detail(id));
    }

    @GetMapping("/recommend")
    public ApiResponse<List<FoodVO>> recommend() {
        return ApiResponse.ok(foodService.recommend());
    }

    @PostMapping("/review")
    public ApiResponse<ActionResultVO> review(@RequestBody @Valid FoodReviewRequest request) {
        return ApiResponse.ok(foodService.review(request));
    }
}
