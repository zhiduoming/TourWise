package com.tourwise.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ItineraryMapper {
    List<Map<String, Object>> listCandidateSpots(
            @Param("userId") Long userId,
            @Param("city") String city,
            @Param("preferences") List<String> preferences,
            @Param("purpose") String purpose,
            @Param("routeRequired") boolean routeRequired,
            @Param("avoidVisited") boolean avoidVisited,
            @Param("limit") int limit);

    List<Map<String, Object>> listCandidateFoods(
            @Param("city") String city,
            @Param("spotIds") List<Long> spotIds,
            @Param("budgetLevel") String budgetLevel,
            @Param("limit") int limit);
}
