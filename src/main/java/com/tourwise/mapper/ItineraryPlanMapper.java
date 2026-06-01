package com.tourwise.mapper;

import com.tourwise.model.ItineraryPlanItemRecord;
import com.tourwise.model.ItineraryPlanRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ItineraryPlanMapper {
    int insertPlan(ItineraryPlanRecord record);

    int insertItem(ItineraryPlanItemRecord record);

    List<Map<String, Object>> listPlans(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long countPlans(@Param("userId") Long userId);

    List<Map<String, Object>> listFavoritePlans(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long countFavoritePlans(@Param("userId") Long userId);

    List<Map<String, Object>> listHotSharedPlans(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    Map<String, Object> findPlanById(
            @Param("id") Long id,
            @Param("userId") Long userId);

    Map<String, Object> findAccessiblePlanById(
            @Param("id") Long id,
            @Param("userId") Long userId);

    Map<String, Object> findNotificationInfo(@Param("id") Long id);

    List<Map<String, Object>> listItems(@Param("planId") Long planId);

    int increaseCopyCount(@Param("id") Long id);

    int insertFavorite(
            @Param("userId") Long userId,
            @Param("planId") Long planId);

    int deleteFavorite(
            @Param("userId") Long userId,
            @Param("planId") Long planId);

    int existsFavorite(
            @Param("userId") Long userId,
            @Param("planId") Long planId);

    int countFavorites(@Param("planId") Long planId);

    int increaseFavoriteCount(@Param("id") Long id);

    int decreaseFavoriteCount(@Param("id") Long id);

    int softDeletePlan(
            @Param("id") Long id,
            @Param("userId") Long userId);
}
