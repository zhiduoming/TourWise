package com.tourwise.mapper;

import com.tourwise.model.RouteMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RouteMapMapper {
    int existsPlaceGroup(@Param("placeGroupId") Long placeGroupId);

    RouteMap findByPlaceGroupId(@Param("placeGroupId") Long placeGroupId);

    int upsert(RouteMap routeMap);
}
