package com.tourwise.controller;

import com.tourwise.dto.*;
import com.tourwise.service.*;

import com.tourwise.common.ApiResponse;
import com.tourwise.common.PageResult;
import com.tourwise.service.AiSummaryService;
import com.tourwise.vo.search.FacilityVO;
import com.tourwise.vo.search.PoiTypeVO;
import com.tourwise.vo.search.SearchTagVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/search")
public class SearchController {
    private final SearchService searchService;
    private final AiSummaryService aiSummaryService;

    public SearchController(SearchService searchService, AiSummaryService aiSummaryService) {
        this.searchService = searchService;
        this.aiSummaryService = aiSummaryService;
    }

    @GetMapping("/types")
    public ApiResponse<List<PoiTypeVO>> listTypes() {
        return ApiResponse.ok(searchService.listTypes());
    }

    @GetMapping("/tags")
    public ApiResponse<List<SearchTagVO>> listSearchTags() {
        return ApiResponse.ok(searchService.listSearchTags());
    }

    @GetMapping("/facilities")
    public ApiResponse<PageResult<FacilityVO>> searchFacilities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) Long placeGroupId,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String shortName,
            @RequestParam(required = false) Boolean spotOnly,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(searchService.searchFacilities(
                keyword,
                type,
                area,
                scene,
                placeGroupId,
                province,
                city,
                tag,
                shortName,
                spotOnly,
                page,
                pageSize));
    }

    @GetMapping("/facility/{id}")
    public ApiResponse<FacilityVO> getFacility(@PathVariable Long id) {
        return ApiResponse.ok(searchService.getFacility(id));
    }

    @GetMapping("/facility/{id}/ai-summary")
    public ApiResponse<Map<String, String>> getAiSummary(@PathVariable Long id) {
        return ApiResponse.ok(Map.of("summary", aiSummaryService.getOrGenerate(id)));
    }
}
