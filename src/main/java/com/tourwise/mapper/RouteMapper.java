package com.tourwise.mapper;

import com.tourwise.model.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RouteMapper {
    List<Map<String, Object>> listRouteSpots();

    List<Map<String, Object>> listRouteScopes();

    List<Map<String, Object>> listPois(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("placeGroupId") Long placeGroupId);

    Long findRouteScopeByPoiId(@Param("spotId") Long spotId);

    RoutePoi findPoiById(@Param("id") Long id, @Param("placeGroupId") Long placeGroupId);

    RoutePoi findPoiByExactName(@Param("name") String name, @Param("placeGroupId") Long placeGroupId);

    RoutePoi findPoiByLikeName(@Param("name") String name, @Param("placeGroupId") Long placeGroupId);

    RoutePoi findNearestPoi(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("placeGroupId") Long placeGroupId);

    List<Map<String, Object>> findNearestSpots(
            @Param("longitude") double longitude,
            @Param("latitude") double latitude);

    List<Map<String, Object>> findNearestInternalPois(
            @Param("placeGroupId") Long placeGroupId,
            @Param("longitude") double longitude,
            @Param("latitude") double latitude);

    List<RouteEdge> listEdges(@Param("indoorOnly") boolean indoorOnly, @Param("placeGroupId") Long placeGroupId);

    List<RoutePoi> listGraphPois(@Param("placeGroupId") Long placeGroupId);
}
