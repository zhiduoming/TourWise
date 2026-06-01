package com.tourwise.mapper;

import com.tourwise.model.RouteEdge;
import com.tourwise.model.RouteGraphEdgeRecord;
import com.tourwise.model.RouteGraphNodeRecord;
import com.tourwise.model.RouteGraphVersionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdminRouteGraphMapper {
    int existsPlaceGroup(@Param("placeGroupId") Long placeGroupId);

    Long findSpotIdByPlaceGroupId(@Param("placeGroupId") Long placeGroupId);

    Long findCategoryIdByCode(@Param("code") String code);

    Map<String, Object> findPlaceGroupSummary(@Param("placeGroupId") Long placeGroupId);

    List<RouteGraphNodeRecord> listNodes(@Param("placeGroupId") Long placeGroupId);

    List<RouteGraphEdgeRecord> listEdges(@Param("placeGroupId") Long placeGroupId);

    int updatePoiCoordinate(
            @Param("id") Long id,
            @Param("placeGroupId") Long placeGroupId,
            @Param("mapX") Integer mapX,
            @Param("mapY") Integer mapY,
            @Param("longitude") BigDecimal longitude,
            @Param("latitude") BigDecimal latitude);

    int deleteEdgesByPlaceGroupId(@Param("placeGroupId") Long placeGroupId);

    int deleteRouteNodesByPlaceGroupId(@Param("placeGroupId") Long placeGroupId);

    int insertRouteNode(RouteGraphNodeRecord node);

    int insertEdge(RouteEdge edge);

    Integer nextVersionNo(@Param("placeGroupId") Long placeGroupId);

    int insertVersion(RouteGraphVersionRecord version);

    List<RouteGraphVersionRecord> listVersions(@Param("placeGroupId") Long placeGroupId);

    RouteGraphVersionRecord findVersion(
            @Param("placeGroupId") Long placeGroupId,
            @Param("versionId") Long versionId);

    Map<String, Object> qualityOverview(@Param("placeGroupId") Long placeGroupId);

    List<Map<String, Object>> listMissingMapPointPois(@Param("placeGroupId") Long placeGroupId);

    List<Map<String, Object>> listIsolatedPois(@Param("placeGroupId") Long placeGroupId);

    List<Map<String, Object>> listIsolatedRouteNodes(@Param("placeGroupId") Long placeGroupId);

    List<Map<String, Object>> listLongEdges(
            @Param("placeGroupId") Long placeGroupId,
            @Param("maxDistanceM") Integer maxDistanceM);

    List<Map<String, Object>> listDuplicateDirectedEdges(@Param("placeGroupId") Long placeGroupId);

    List<Map<String, Object>> listSelfLoopEdges(@Param("placeGroupId") Long placeGroupId);

    List<Map<String, Object>> listInvalidEdges(@Param("placeGroupId") Long placeGroupId);

    List<Map<String, Object>> listCrossGroupEdges(@Param("placeGroupId") Long placeGroupId);

    int deleteInvalidEdges(@Param("placeGroupId") Long placeGroupId);

    int deleteCrossGroupEdges(@Param("placeGroupId") Long placeGroupId);

    int deleteSelfLoopEdges(@Param("placeGroupId") Long placeGroupId);

    int deleteDuplicateDirectedEdges(@Param("placeGroupId") Long placeGroupId);
}
