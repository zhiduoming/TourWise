package com.tourwise.service;

import com.tourwise.dto.*;
import com.tourwise.mapper.*;
import com.tourwise.model.*;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.common.TopKSelector;
import com.tourwise.security.AuthContext;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.food.FoodVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FoodService {
    private final FoodMapper foodMapper;
    private final AiSummaryService aiSummaryService;

    public FoodService(FoodMapper foodMapper, AiSummaryService aiSummaryService) {
        this.foodMapper = foodMapper;
        this.aiSummaryService = aiSummaryService;
    }

    public Map<String, Object> pagedList(String cuisine, String price, String sort,
                                         Long near, Long spotId, Long placeGroupId,
                                         Integer page, Integer pageSize) {
        Integer priceLevel = parseInt(price);
        String normalizedSort = normalizeSort(sort);
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeSize = pageSize == null || pageSize <= 0 ? 12 : Math.min(pageSize, 60);
        int offset = (safePage - 1) * safeSize;

        List<FoodVO> list = foodMapper.pagedList(
                trimToNull(cuisine), priceLevel, normalizedSort,
                normalizeId(near), normalizeId(spotId), normalizeId(placeGroupId),
                offset, safeSize
        ).stream().map(FoodService::normalizeFood).map(FoodVO::from).toList();

        long total = foodMapper.pagedCount(
                trimToNull(cuisine), priceLevel, normalizedSort,
                normalizeId(near), normalizeId(spotId), normalizeId(placeGroupId)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", safePage);
        result.put("pageSize", safeSize);
        return result;
    }

    public String getOrGenerateAiSummary(Long foodId, boolean force) {
        if (foodMapper.exists(foodId) == 0) {
            throw BusinessException.notFound("美食不存在");
        }
        return aiSummaryService.getOrGenerateForFood(foodId, force);
    }

    public List<FoodVO> list(String cuisine, String price, String sort, Long near, Long spotId, Long placeGroupId, Integer limit) {
        Integer priceLevel = parseInt(price);
        Long exactPoiId = near;
        String normalizedSort = normalizeSort(sort);
        int normalizedLimit = limit == null || limit <= 0 ? 100 : Math.min(limit, 100);
        return foodMapper.list(
                trimToNull(cuisine),
                priceLevel,
                normalizedSort,
                normalizeId(exactPoiId),
                normalizeId(spotId),
                normalizeId(placeGroupId),
                normalizedLimit).stream().map(FoodService::normalizeFood).map(FoodVO::from).toList();
    }

    public FoodVO detail(Long id) {
        Map<String, Object> row = foodMapper.detail(id);
        if (row == null) {
            throw BusinessException.notFound("美食不存在");
        }
        return FoodVO.from(normalizeFood(row));
    }

    public List<FoodVO> recommend() {
        // 课设要求 (5)-①：用户通常只看前 10，要求不经过完全排序排好前 10。
        // 用大小为 K=10 的最小堆，时间 O(N log K) 远优于全排序 O(N log N)。
        // 综合得分 = 评分 * 20 * 0.6 + 热度 * 0.4（评分 0-5 归一到 0-100）。
        List<Map<String, Object>> candidates = foodMapper.recommendCandidates();
        Comparator<Map<String, Object>> byScore = Comparator.comparingDouble(FoodService::compositeFoodScore);
        TopKSelector<Map<String, Object>> selector = new TopKSelector<>(10, byScore);
        for (Map<String, Object> row : candidates) {
            selector.offer(normalizeFood(row));
        }
        return selector.toListDescending().stream().map(row -> {
            row.put("recommendReason", "评分和人气表现较好，适合优先尝试");
            return FoodVO.from(row);
        }).toList();
    }

    /** 美食推荐综合得分：把评分和热度按经验权重融合，便于堆排序按单一维度比较。 */
    private static double compositeFoodScore(Map<String, Object> row) {
        double rating = toDouble(row.get("rating"));
        double hotness = toDouble(row.get("hotness"));
        return rating * 20.0 * 0.6 + hotness * 0.4;
    }

    private static double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    public ActionResultVO review(FoodReviewRequest request) {
        long userId = AuthContext.requireUserId();
        if (foodMapper.exists(request.getFoodId()) == 0) {
            throw BusinessException.notFound("美食不存在");
        }
        FoodReviewRecord record = new FoodReviewRecord();
        record.setFoodId(request.getFoodId());
        record.setUserId(userId);
        record.setRating(request.getRating());
        record.setContent(request.getContent().trim());
        foodMapper.insertReview(record);
        BigDecimal rating = record.getRating();
        if (rating != null) {
            foodMapper.refreshRating(request.getFoodId());
        }
        return ActionResultVO.created("reviewId", record.getId());
    }

    private static String normalizeSort(String sort) {
        String value = trimToNull(sort);
        if (value == null) {
            return "score";
        }
        return switch (value) {
            case "distance", "popular", "score" -> value;
            default -> "score";
        };
    }

    private static Integer parseInt(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Long normalizeId(Long value) {
        return value == null || value <= 0 ? null : value;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static Map<String, Object> normalizeFood(Map<String, Object> row) {
        Map<String, Object> item = MapUtil.normalize(row);
        if (item.containsKey("cuisineType")) {
            item.put("cuisine_type", item.get("cuisineType"));
        }
        if (item.containsKey("spotId")) {
            item.put("spot_id", item.get("spotId"));
        }
        if (item.containsKey("spotName")) {
            item.put("spot_name", item.get("spotName"));
        }
        return item;
    }
}
