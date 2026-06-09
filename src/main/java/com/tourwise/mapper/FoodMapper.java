package com.tourwise.mapper;

import com.tourwise.model.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface FoodMapper {
    List<Map<String, Object>> list(
            @Param("cuisine") String cuisine,
            @Param("priceLevel") Integer priceLevel,
            @Param("sort") String sort,
            @Param("poiId") Long poiId,
            @Param("spotId") Long spotId,
            @Param("placeGroupId") Long placeGroupId,
            @Param("limit") int limit);

    Map<String, Object> detail(@Param("id") Long id);

    List<Map<String, Object>> pagedList(
            @Param("cuisine") String cuisine,
            @Param("priceLevel") Integer priceLevel,
            @Param("sort") String sort,
            @Param("poiId") Long poiId,
            @Param("spotId") Long spotId,
            @Param("placeGroupId") Long placeGroupId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long pagedCount(
            @Param("cuisine") String cuisine,
            @Param("priceLevel") Integer priceLevel,
            @Param("sort") String sort,
            @Param("poiId") Long poiId,
            @Param("spotId") Long spotId,
            @Param("placeGroupId") Long placeGroupId);

    String findAiSummary(@Param("id") Long id);

    int saveAiSummary(@Param("id") Long id, @Param("summary") String summary);

    List<Map<String, Object>> recommend();

    List<Map<String, Object>> recommendCandidates();

    int exists(@Param("id") Long id);

    int insertReview(FoodReviewRecord record);

    int refreshRating(@Param("foodId") Long foodId);
}
