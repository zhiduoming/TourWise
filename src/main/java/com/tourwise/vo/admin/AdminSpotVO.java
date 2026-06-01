package com.tourwise.vo.admin;

import com.tourwise.vo.VoConvert;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class AdminSpotVO {
    private Long id;
    private Long placeGroupId;
    private Long representativePoiId;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String shortName;
    private String spotType;
    private String province;
    private String city;
    private String district;
    private String address;
    private String description;
    private String coverImage;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer locationRadiusM;
    private BigDecimal rating;
    private Integer hotness;
    private Integer visitCount;
    private Integer status;
    private String placeGroupName;
    private String routeGraphStatus;
    private Integer poiCount;
    private Integer tagCount;

    public static AdminSpotVO from(Map<String, Object> row) {
        AdminSpotVO vo = new AdminSpotVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setPlaceGroupId(VoConvert.longValue(row, "placeGroupId"));
        vo.setRepresentativePoiId(VoConvert.longValue(row, "representativePoiId"));
        vo.setCategoryId(VoConvert.longValue(row, "categoryId"));
        vo.setCategoryName(VoConvert.string(row, "categoryName"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setShortName(VoConvert.string(row, "shortName"));
        vo.setSpotType(VoConvert.string(row, "spotType"));
        vo.setProvince(VoConvert.string(row, "province"));
        vo.setCity(VoConvert.string(row, "city"));
        vo.setDistrict(VoConvert.string(row, "district"));
        vo.setAddress(VoConvert.string(row, "address"));
        vo.setDescription(VoConvert.string(row, "description"));
        vo.setCoverImage(VoConvert.string(row, "coverImage"));
        vo.setLongitude(VoConvert.decimal(row, "longitude"));
        vo.setLatitude(VoConvert.decimal(row, "latitude"));
        vo.setLocationRadiusM(VoConvert.intValue(row, "locationRadiusM"));
        vo.setRating(VoConvert.decimal(row, "rating"));
        vo.setHotness(VoConvert.intValue(row, "hotness"));
        vo.setVisitCount(VoConvert.intValue(row, "visitCount"));
        vo.setStatus(VoConvert.intValue(row, "status"));
        vo.setPlaceGroupName(VoConvert.string(row, "placeGroupName"));
        vo.setRouteGraphStatus(VoConvert.string(row, "routeGraphStatus"));
        vo.setPoiCount(VoConvert.intValue(row, "poiCount"));
        vo.setTagCount(VoConvert.intValue(row, "tagCount"));
        return vo;
    }
}
