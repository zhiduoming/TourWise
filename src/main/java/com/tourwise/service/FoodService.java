package com.tourwise.service;

import com.tourwise.dto.*;
import com.tourwise.mapper.*;
import com.tourwise.model.*;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.security.AuthContext;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.food.FoodVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class FoodService {
    private final FoodMapper foodMapper;

    public FoodService(FoodMapper foodMapper) {
        this.foodMapper = foodMapper;
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
        return foodMapper.recommend().stream().map(row -> {
            Map<String, Object> item = normalizeFood(row);
            item.put("recommendReason", "评分和人气表现较好，适合优先尝试");
            return FoodVO.from(item);
        }).toList();
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
