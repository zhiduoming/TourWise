package com.tourwise.mapper;

import com.tourwise.model.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface RecommendMapper {
    List<Map<String, Object>> hotTop10();

    /**
     * 返回所有候选景区/校园（不在 SQL 端排序、不 LIMIT），供 Java 端用 TopK 堆排序。
     */
    List<Map<String, Object>> hotCandidates();

    List<Map<String, Object>> list(
            @Param("userId") Long userId,
            @Param("strategy") String strategy,
            @Param("scene") String scene,
            @Param("purpose") String purpose,
            @Param("city") String city,
            @Param("routeRequired") boolean routeRequired,
            @Param("foodRequired") boolean foodRequired,
            @Param("avoidVisited") boolean avoidVisited,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long count(
            @Param("userId") Long userId,
            @Param("scene") String scene,
            @Param("city") String city,
            @Param("routeRequired") boolean routeRequired,
            @Param("foodRequired") boolean foodRequired,
            @Param("avoidVisited") boolean avoidVisited);

    default boolean targetExists(String targetType, Long targetId) {
        return switch (targetType) {
            case "poi" -> existsPoi(targetId) > 0;
            case "food" -> existsFood(targetId) > 0;
            case "log" -> existsLog(targetId) > 0;
            default -> false;
        };
    }

    int existsPoi(@Param("id") Long id);

    int existsFood(@Param("id") Long id);

    int existsLog(@Param("id") Long id);

    int upsertRating(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("rating") BigDecimal rating);

    int refreshPoiRating(@Param("id") Long id);

    int refreshFoodRating(@Param("id") Long id);
}
