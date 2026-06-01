package com.tourwise.mapper;

import com.tourwise.model.RouteGraphEdgeRecord;
import com.tourwise.model.RouteGraphNodeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminDashboardMapper {
    Map<String, Object> overview();

    List<Map<String, Object>> listSpotQuality();

    List<Map<String, Object>> listRouteGraphScopes();

    List<RouteGraphNodeRecord> listQualityNodes(@Param("placeGroupId") Long placeGroupId);

    List<RouteGraphEdgeRecord> listQualityEdges(@Param("placeGroupId") Long placeGroupId);
}
