package com.tourwise.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserSpotActionMapper {
    int existsPoi(@Param("targetId") Long targetId);

    int existsFavorite(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId);

    void insertFavorite(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId);

    int deleteFavorite(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId);

    Integer findActionActive(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("actionType") String actionType);

    void upsertAction(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("actionType") String actionType,
            @Param("active") boolean active);

    void insertBrowsingHistory(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId);

    int upsertPreferenceFromPoi(
            @Param("userId") Long userId,
            @Param("targetId") Long targetId,
            @Param("source") String source,
            @Param("weight") java.math.BigDecimal weight);

    int incrementPoiVisit(@Param("targetId") Long targetId);

    int incrementSpotVisitByPoi(@Param("targetId") Long targetId);

    List<Map<String, Object>> listUserActionPois(
            @Param("userId") Long userId,
            @Param("actionType") String actionType,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long countUserActionPois(
            @Param("userId") Long userId,
            @Param("actionType") String actionType);

    List<Map<String, Object>> listFavoritePois(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long countFavoritePois(@Param("userId") Long userId);

    List<Map<String, Object>> listBrowsingPois(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long countBrowsingPois(@Param("userId") Long userId);
}
