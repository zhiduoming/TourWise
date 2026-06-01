package com.tourwise.mapper;

import com.tourwise.dto.AdminPoiRequest;
import com.tourwise.dto.AdminSpotCreateRequest;
import com.tourwise.dto.AdminSpotUpdateRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminSpotMapper {
    List<Map<String, Object>> listSpots(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("routeGraphStatus") String routeGraphStatus,
            @Param("qualityIssue") String qualityIssue,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long countSpots(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("routeGraphStatus") String routeGraphStatus,
            @Param("qualityIssue") String qualityIssue);

    Map<String, Object> findSpotById(@Param("id") Long id);

    Long findCategoryIdByCode(@Param("code") String code);

    int insertPlaceGroup(
            @Param("request") AdminSpotCreateRequest request,
            @Param("groupType") String groupType);

    int insertSpot(@Param("request") AdminSpotCreateRequest request);

    int insertRepresentativePoi(
            @Param("spotId") Long spotId,
            @Param("placeGroupId") Long placeGroupId,
            @Param("request") AdminSpotCreateRequest request,
            @Param("scene") String scene,
            @Param("areaCode") String areaCode,
            @Param("areaName") String areaName);

    int updateSpotRepresentativePoi(
            @Param("spotId") Long spotId,
            @Param("representativePoiId") Long representativePoiId);

    int updateSpot(@Param("id") Long id, @Param("request") AdminSpotUpdateRequest request);

    int updateSpotStatus(@Param("id") Long id, @Param("status") Integer status);

    int updatePlaceGroupFromSpot(@Param("placeGroupId") Long placeGroupId, @Param("request") AdminSpotUpdateRequest request);

    int updateRepresentativePoiFromSpot(
            @Param("poiId") Long poiId,
            @Param("request") AdminSpotUpdateRequest request,
            @Param("scene") String scene,
            @Param("areaCode") String areaCode,
            @Param("areaName") String areaName);

    int updateRepresentativePoiStatus(@Param("poiId") Long poiId, @Param("status") Integer status);

    List<Map<String, Object>> listPois(
            @Param("spotId") Long spotId,
            @Param("placeGroupId") Long placeGroupId,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("qualityIssue") String qualityIssue,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long countPois(
            @Param("spotId") Long spotId,
            @Param("placeGroupId") Long placeGroupId,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("qualityIssue") String qualityIssue);

    Map<String, Object> findPoiById(@Param("id") Long id);

    int insertPoi(
            @Param("spotId") Long spotId,
            @Param("placeGroupId") Long placeGroupId,
            @Param("request") AdminPoiRequest request);

    int updatePoi(@Param("id") Long id, @Param("request") AdminPoiRequest request);

    int updatePoiStatus(@Param("id") Long id, @Param("status") Integer status);

    List<Map<String, Object>> listCategories();

    List<Map<String, Object>> listTags(@Param("tagType") String tagType);

    List<Map<String, Object>> listSpotTags(@Param("spotId") Long spotId);

    Long findTagId(@Param("name") String name, @Param("tagType") String tagType);

    int insertTag(@Param("name") String name, @Param("tagType") String tagType);

    int deleteSpotTags(@Param("spotId") Long spotId);

    int insertSpotTag(@Param("spotId") Long spotId, @Param("tagId") Long tagId);
}
