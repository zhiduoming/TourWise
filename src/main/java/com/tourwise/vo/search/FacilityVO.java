package com.tourwise.vo.search;

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
public class FacilityVO {
    private Long id;
    private String name;
    private String type;
    private String typeName;
    private String category;
    private String categoryName;
    private String description;
    private String location;
    private String area;
    private String areaName;
    private String address;
    private String image;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String openTime;
    private String closeTime;
    private String phone;
    private BigDecimal rating;
    private BigDecimal score;
    private Integer hotness;
    private Integer visits;
    private Long placeGroupId;
    private String placeGroupName;
    private String shortName;
    private String province;
    private String city;
    private String district;
    private Integer distance;

    public static FacilityVO from(Map<String, Object> row) {
        FacilityVO vo = new FacilityVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setType(VoConvert.string(row, "type"));
        vo.setTypeName(VoConvert.string(row, "typeName"));
        vo.setCategory(VoConvert.string(row, "category"));
        vo.setCategoryName(VoConvert.string(row, "categoryName"));
        vo.setDescription(VoConvert.string(row, "description"));
        vo.setLocation(VoConvert.string(row, "location"));
        vo.setArea(VoConvert.string(row, "area"));
        vo.setAreaName(VoConvert.string(row, "areaName"));
        vo.setAddress(VoConvert.string(row, "address"));
        vo.setImage(VoConvert.string(row, "image"));
        vo.setLongitude(VoConvert.decimal(row, "longitude"));
        vo.setLatitude(VoConvert.decimal(row, "latitude"));
        vo.setOpenTime(VoConvert.string(row, "openTime"));
        vo.setCloseTime(VoConvert.string(row, "closeTime"));
        vo.setPhone(VoConvert.string(row, "phone"));
        vo.setRating(VoConvert.decimal(row, "rating"));
        vo.setScore(VoConvert.decimal(row, "score"));
        vo.setHotness(VoConvert.intValue(row, "hotness"));
        vo.setVisits(VoConvert.intValue(row, "visits"));
        vo.setPlaceGroupId(VoConvert.longValue(row, "placeGroupId"));
        vo.setPlaceGroupName(VoConvert.string(row, "placeGroupName"));
        vo.setShortName(VoConvert.string(row, "shortName"));
        vo.setProvince(VoConvert.string(row, "province"));
        vo.setCity(VoConvert.string(row, "city"));
        vo.setDistrict(VoConvert.string(row, "district"));
        vo.setDistance(VoConvert.intValue(row, "distance"));
        return vo;
    }
}
