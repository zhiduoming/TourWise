package com.tourwise.mapper;

import com.tourwise.model.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SearchMapper {
    List<Map<String, Object>> listTypes();

    List<Map<String, Object>> searchFacilities(
            @Param("keyword") String keyword,
            @Param("type") String type,
            @Param("area") String area,
            @Param("scene") String scene,
            @Param("placeGroupId") Long placeGroupId,
            @Param("province") String province,
            @Param("city") String city,
            @Param("tag") String tag,
            @Param("shortName") String shortName,
            @Param("spotOnly") Boolean spotOnly,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long countFacilities(
            @Param("keyword") String keyword,
            @Param("type") String type,
            @Param("area") String area,
            @Param("scene") String scene,
            @Param("placeGroupId") Long placeGroupId,
            @Param("province") String province,
            @Param("city") String city,
            @Param("tag") String tag,
            @Param("shortName") String shortName,
            @Param("spotOnly") Boolean spotOnly);

    Map<String, Object> findFacilityById(@Param("id") Long id);

    String findAiSummary(@Param("id") Long id);

    void saveAiSummary(@Param("id") Long id, @Param("summary") String summary);
}
