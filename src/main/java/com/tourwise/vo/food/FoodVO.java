package com.tourwise.vo.food;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class FoodVO {
    private Long id;
    private String name;
    private String image;
    private String description;
    private BigDecimal score;
    private BigDecimal rating;
    private Integer priceLevel;
    private BigDecimal avgPrice;
    private Integer distance;
    private String cuisine;
    private String cuisineName;
    private String cuisineType;
    private Boolean recommend;
    private String address;
    private String openTime;
    private String closeTime;
    private String phone;
    private Integer hotness;
    private Long spotId;
    private String spotName;
    private String recommendReason;

    public static FoodVO from(Map<String, Object> row) {
        FoodVO vo = new FoodVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setImage(VoConvert.string(row, "image"));
        vo.setDescription(VoConvert.string(row, "description"));
        vo.setScore(VoConvert.decimal(row, "score"));
        vo.setRating(VoConvert.decimal(row, "rating"));
        vo.setPriceLevel(VoConvert.intValue(row, "priceLevel"));
        vo.setAvgPrice(VoConvert.decimal(row, "avgPrice"));
        vo.setDistance(VoConvert.intValue(row, "distance"));
        vo.setCuisine(VoConvert.string(row, "cuisine"));
        vo.setCuisineName(VoConvert.string(row, "cuisineName"));
        vo.setCuisineType(firstString(row, "cuisineType", "cuisine_type"));
        vo.setRecommend(VoConvert.bool(row, "recommend"));
        vo.setAddress(VoConvert.string(row, "address"));
        vo.setOpenTime(VoConvert.string(row, "openTime"));
        vo.setCloseTime(VoConvert.string(row, "closeTime"));
        vo.setPhone(VoConvert.string(row, "phone"));
        vo.setHotness(VoConvert.intValue(row, "hotness"));
        vo.setSpotId(firstLong(row, "spotId", "spot_id"));
        vo.setSpotName(firstString(row, "spotName", "spot_name"));
        vo.setRecommendReason(VoConvert.string(row, "recommendReason"));
        return vo;
    }

    @JsonProperty("cuisine_type")
    public String getCuisineTypeAlias() {
        return cuisineType;
    }

    @JsonProperty("spot_id")
    public Long getSpotIdAlias() {
        return spotId;
    }

    @JsonProperty("spot_name")
    public String getSpotNameAlias() {
        return spotName;
    }

    private static String firstString(Map<String, Object> row, String first, String second) {
        String firstValue = VoConvert.string(row, first);
        return firstValue != null ? firstValue : VoConvert.string(row, second);
    }

    private static Long firstLong(Map<String, Object> row, String first, String second) {
        Long firstValue = VoConvert.longValue(row, first);
        return firstValue != null ? firstValue : VoConvert.longValue(row, second);
    }
}
