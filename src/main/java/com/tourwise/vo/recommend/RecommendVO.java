package com.tourwise.vo.recommend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tourwise.vo.VoConvert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendVO {
    private Long id;
    private Long spotId;
    private String name;
    private String image;
    private String description;
    private String category;
    private String categoryName;
    private BigDecimal score;
    private BigDecimal rating;
    private Integer distance;
    private Integer visits;
    private Integer hotness;
    private Long placeGroupId;
    private String spotType;
    private String province;
    private String city;
    private String routeGraphStatus;
    private Integer foodCount;
    private Integer logCount;
    private String tagNames;
    private String openTime;
    private String closeTime;
    private BigDecimal recommendScore;
    private String recommendReason;
    private Boolean favoriteMatched;
    private Boolean wantMatched;
    private Boolean visitedMatched;
    private Boolean dislikeMatched;
    private Integer browseCount;
    private BigDecimal userRating;
    private BigDecimal preferenceScore;
    private Integer similarActionCount;

    public static RecommendVO from(Map<String, Object> row) {
        RecommendVO vo = new RecommendVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setSpotId(VoConvert.longValue(row, "spotId"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setImage(VoConvert.string(row, "image"));
        vo.setDescription(VoConvert.string(row, "description"));
        vo.setCategory(VoConvert.string(row, "category"));
        vo.setCategoryName(VoConvert.string(row, "categoryName"));
        vo.setScore(VoConvert.decimal(row, "score"));
        vo.setRating(VoConvert.decimal(row, "rating"));
        vo.setDistance(VoConvert.intValue(row, "distance"));
        vo.setVisits(VoConvert.intValue(row, "visits"));
        vo.setHotness(VoConvert.intValue(row, "hotness"));
        vo.setPlaceGroupId(VoConvert.longValue(row, "placeGroupId"));
        vo.setSpotType(VoConvert.string(row, "spotType"));
        vo.setProvince(VoConvert.string(row, "province"));
        vo.setCity(VoConvert.string(row, "city"));
        vo.setRouteGraphStatus(VoConvert.string(row, "routeGraphStatus"));
        vo.setFoodCount(VoConvert.intValue(row, "foodCount"));
        vo.setLogCount(VoConvert.intValue(row, "logCount"));
        vo.setTagNames(VoConvert.string(row, "tagNames"));
        vo.setOpenTime(VoConvert.string(row, "openTime"));
        vo.setCloseTime(VoConvert.string(row, "closeTime"));
        vo.setRecommendScore(VoConvert.decimal(row, "recommendScore"));
        vo.setRecommendReason(VoConvert.string(row, "recommendReason"));
        vo.setFavoriteMatched(VoConvert.bool(row, "favoriteMatched"));
        vo.setWantMatched(VoConvert.bool(row, "wantMatched"));
        vo.setVisitedMatched(VoConvert.bool(row, "visitedMatched"));
        vo.setDislikeMatched(VoConvert.bool(row, "dislikeMatched"));
        vo.setBrowseCount(VoConvert.intValue(row, "browseCount"));
        vo.setUserRating(VoConvert.decimal(row, "userRating"));
        vo.setPreferenceScore(VoConvert.decimal(row, "preferenceScore"));
        vo.setSimilarActionCount(VoConvert.intValue(row, "similarActionCount"));
        return vo;
    }
}
