package com.tourwise.vo.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tourwise.vo.VoConvert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogVO {
    private Long id;
    private Long userId;
    private String username;
    private String avatar;
    private Long spotId;
    private Long circleId;
    private String location;
    private Long itineraryPlanId;
    private String itineraryPlanTitle;
    private String itineraryPlanCity;
    private String itineraryPlanDuration;
    private Integer itineraryPlanSpotCount;
    private String itineraryPlanSummary;
    private String title;
    private String content;
    private BigDecimal rating;
    private BigDecimal sceneryRating;
    private BigDecimal facilityRating;
    private BigDecimal serviceRating;
    private BigDecimal trafficRating;
    private BigDecimal valueRating;
    private Integer hotness;
    private Integer viewCount;
    private Integer isTop;
    private String createdAt;
    private Integer likeCount;
    private Integer commentCount;
    private Integer isLiked;
    private List<String> images;
    private List<String> tags;

    public static LogVO from(Map<String, Object> row) {
        LogVO vo = new LogVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setUserId(firstLong(row, "userId", "user_id"));
        vo.setUsername(VoConvert.string(row, "username"));
        vo.setAvatar(VoConvert.string(row, "avatar"));
        vo.setSpotId(firstLong(row, "spotId", "spot_id"));
        vo.setCircleId(firstLong(row, "circleId", "circle_id"));
        vo.setLocation(VoConvert.string(row, "location"));
        vo.setItineraryPlanId(VoConvert.longValue(row, "itineraryPlanId"));
        vo.setItineraryPlanTitle(VoConvert.string(row, "itineraryPlanTitle"));
        vo.setItineraryPlanCity(VoConvert.string(row, "itineraryPlanCity"));
        vo.setItineraryPlanDuration(VoConvert.string(row, "itineraryPlanDuration"));
        vo.setItineraryPlanSpotCount(VoConvert.intValue(row, "itineraryPlanSpotCount"));
        vo.setItineraryPlanSummary(VoConvert.string(row, "itineraryPlanSummary"));
        vo.setTitle(VoConvert.string(row, "title"));
        vo.setContent(VoConvert.string(row, "content"));
        vo.setRating(VoConvert.decimal(row, "rating"));
        vo.setSceneryRating(firstDecimal(row, "sceneryRating", "scenery_rating"));
        vo.setFacilityRating(firstDecimal(row, "facilityRating", "facility_rating"));
        vo.setServiceRating(firstDecimal(row, "serviceRating", "service_rating"));
        vo.setTrafficRating(firstDecimal(row, "trafficRating", "traffic_rating"));
        vo.setValueRating(firstDecimal(row, "valueRating", "value_rating"));
        vo.setHotness(VoConvert.intValue(row, "hotness"));
        vo.setViewCount(firstInt(row, "viewCount", "view_count"));
        vo.setIsTop(firstInt(row, "isTop", "is_top"));
        vo.setCreatedAt(firstString(row, "createdAt", "created_at"));
        vo.setLikeCount(firstInt(row, "likeCount", "like_count"));
        vo.setCommentCount(firstInt(row, "commentCount", "comment_count"));
        vo.setIsLiked(firstInt(row, "isLiked", "is_liked"));
        vo.setImages(stringList(row.get("images")));
        vo.setTags(stringList(row.get("tags")));
        return vo;
    }

    @JsonProperty("user_id")
    public Long getUserIdAlias() {
        return userId;
    }

    @JsonProperty("spot_id")
    public Long getSpotIdAlias() {
        return spotId;
    }

    @JsonProperty("circle_id")
    public Long getCircleIdAlias() {
        return circleId;
    }

    @JsonProperty("itinerary_plan_id")
    public Long getItineraryPlanIdAlias() {
        return itineraryPlanId;
    }

    @JsonProperty("view_count")
    public Integer getViewCountAlias() {
        return viewCount;
    }

    @JsonProperty("is_top")
    public Integer getIsTopAlias() {
        return isTop;
    }

    @JsonProperty("created_at")
    public String getCreatedAtAlias() {
        return createdAt;
    }

    @JsonProperty("scenery_rating")
    public BigDecimal getSceneryRatingAlias() {
        return sceneryRating;
    }

    @JsonProperty("facility_rating")
    public BigDecimal getFacilityRatingAlias() {
        return facilityRating;
    }

    @JsonProperty("service_rating")
    public BigDecimal getServiceRatingAlias() {
        return serviceRating;
    }

    @JsonProperty("traffic_rating")
    public BigDecimal getTrafficRatingAlias() {
        return trafficRating;
    }

    @JsonProperty("value_rating")
    public BigDecimal getValueRatingAlias() {
        return valueRating;
    }

    @JsonProperty("like_count")
    public Integer getLikeCountAlias() {
        return likeCount;
    }

    @JsonProperty("comment_count")
    public Integer getCommentCountAlias() {
        return commentCount;
    }

    @JsonProperty("is_liked")
    public Integer getIsLikedAlias() {
        return isLiked;
    }

    private static Long firstLong(Map<String, Object> row, String first, String second) {
        Long firstValue = VoConvert.longValue(row, first);
        return firstValue != null ? firstValue : VoConvert.longValue(row, second);
    }

    private static Integer firstInt(Map<String, Object> row, String first, String second) {
        Integer firstValue = VoConvert.intValue(row, first);
        return firstValue != null ? firstValue : VoConvert.intValue(row, second);
    }

    private static BigDecimal firstDecimal(Map<String, Object> row, String first, String second) {
        BigDecimal firstValue = VoConvert.decimal(row, first);
        return firstValue != null ? firstValue : VoConvert.decimal(row, second);
    }

    private static String firstString(Map<String, Object> row, String first, String second) {
        String firstValue = VoConvert.string(row, first);
        return firstValue != null ? firstValue : VoConvert.string(row, second);
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
