package com.tourwise.vo.admin;

import com.tourwise.vo.VoConvert;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DataQualityVO {
    private Long spotId;
    private Long placeGroupId;
    private Long representativePoiId;
    private String name;
    private String spotType;
    private String province;
    private String city;
    private Integer status;
    private String routeGraphStatus;
    private Integer visiblePoiCount;
    private Integer tagCount;
    private Integer routeIssueCount;
    private Integer completenessScore;
    private Integer completedItemCount;
    private Integer totalItemCount;
    private Boolean missingCategory;
    private Boolean missingCover;
    private Boolean missingGeo;
    private Boolean missingShortName;
    private Boolean missingDescription;
    private Boolean missingAddress;
    private Boolean missingTags;
    private Boolean missingRepresentativePoi;
    private Boolean representativePoiDisabled;
    private Boolean missingVisiblePoi;
    private Boolean campusRouteMissing;
    private String level;
    private List<String> issues;

    public static DataQualityVO from(Map<String, Object> row) {
        DataQualityVO vo = new DataQualityVO();
        vo.setSpotId(VoConvert.longValue(row, "spotId"));
        vo.setPlaceGroupId(VoConvert.longValue(row, "placeGroupId"));
        vo.setRepresentativePoiId(VoConvert.longValue(row, "representativePoiId"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setSpotType(VoConvert.string(row, "spotType"));
        vo.setProvince(VoConvert.string(row, "province"));
        vo.setCity(VoConvert.string(row, "city"));
        vo.setStatus(VoConvert.intValue(row, "status"));
        vo.setRouteGraphStatus(VoConvert.string(row, "routeGraphStatus"));
        vo.setVisiblePoiCount(VoConvert.intValue(row, "visiblePoiCount"));
        vo.setTagCount(VoConvert.intValue(row, "tagCount"));
        vo.setRouteIssueCount(0);
        vo.setMissingCategory(VoConvert.bool(row, "missingCategory"));
        vo.setMissingCover(VoConvert.bool(row, "missingCover"));
        vo.setMissingGeo(VoConvert.bool(row, "missingGeo"));
        vo.setMissingShortName(VoConvert.bool(row, "missingShortName"));
        vo.setMissingDescription(VoConvert.bool(row, "missingDescription"));
        vo.setMissingAddress(VoConvert.bool(row, "missingAddress"));
        vo.setMissingTags(VoConvert.bool(row, "missingTags"));
        vo.setMissingRepresentativePoi(VoConvert.bool(row, "missingRepresentativePoi"));
        vo.setRepresentativePoiDisabled(VoConvert.bool(row, "representativePoiDisabled"));
        vo.setMissingVisiblePoi(VoConvert.bool(row, "missingVisiblePoi"));
        vo.setCampusRouteMissing(VoConvert.bool(row, "campusRouteMissing"));
        return vo;
    }
}
