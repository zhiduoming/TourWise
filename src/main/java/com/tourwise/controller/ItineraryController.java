package com.tourwise.controller;

import com.tourwise.common.ApiResponse;
import com.tourwise.common.PageResult;
import com.tourwise.dto.ItineraryGenerateRequest;
import com.tourwise.dto.ItineraryPlanSaveRequest;
import com.tourwise.service.ItineraryService;
import com.tourwise.vo.itinerary.ItineraryFavoriteStateVO;
import com.tourwise.vo.itinerary.ItineraryPlanVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/itinerary")
public class ItineraryController {
    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    @PostMapping("/generate")
    public ApiResponse<ItineraryPlanVO> generate(@RequestBody @Valid ItineraryGenerateRequest request) {
        return ApiResponse.ok(itineraryService.generate(request));
    }

    @PostMapping("/plans")
    public ApiResponse<ItineraryPlanVO> save(@RequestBody @Valid ItineraryPlanSaveRequest request) {
        return ApiResponse.ok(itineraryService.save(request));
    }

    @GetMapping("/plans")
    public ApiResponse<PageResult<ItineraryPlanVO>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(itineraryService.list(page, pageSize));
    }

    @GetMapping("/plans/favorites")
    public ApiResponse<PageResult<ItineraryPlanVO>> favoritePlans(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(itineraryService.favoritePlans(page, pageSize));
    }

    @GetMapping("/shared-plans/hot")
    public ApiResponse<java.util.List<ItineraryPlanVO>> hotSharedPlans(
            @RequestParam(defaultValue = "8") @Min(1) @Max(20) int limit) {
        return ApiResponse.ok(itineraryService.hotSharedPlans(limit));
    }

    @GetMapping("/plans/{id}")
    public ApiResponse<ItineraryPlanVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(itineraryService.detail(id));
    }

    @GetMapping("/shared-plans/{id}")
    public ApiResponse<ItineraryPlanVO> sharedDetail(@PathVariable Long id) {
        return ApiResponse.ok(itineraryService.sharedDetail(id));
    }

    @PostMapping("/plans/{id}/copy")
    public ApiResponse<ItineraryPlanVO> copy(@PathVariable Long id) {
        return ApiResponse.ok(itineraryService.copy(id));
    }

    @PostMapping("/plans/{id}/favorite")
    public ApiResponse<ItineraryFavoriteStateVO> favorite(@PathVariable Long id) {
        return ApiResponse.ok(itineraryService.toggleFavorite(id));
    }

    @DeleteMapping("/plans/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        itineraryService.delete(id);
        return ApiResponse.ok(null);
    }
}
