package com.tourwise.vo.admin;

import com.tourwise.vo.VoConvert;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class AdminPoiVO {
    private Long id;
    private Long spotId;
    private Long placeGroupId;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String name;
    private String scene;
    private String areaCode;
    private String areaName;
    private String province;
    private String city;
    private String address;
    private String locationText;
    private String description;
    private String imageUrl;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer mapX;
    private Integer mapY;
    private BigDecimal rating;
    private Integer hotness;
    private Integer visitCount;
    private Integer routeEdgeCount;
    private Integer status;

    public static AdminPoiVO from(Map<String, Object> row) {
        AdminPoiVO vo = new AdminPoiVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setSpotId(VoConvert.longValue(row, "spotId"));
        vo.setPlaceGroupId(VoConvert.longValue(row, "placeGroupId"));
        vo.setCategoryId(VoConvert.longValue(row, "categoryId"));
        vo.setCategoryCode(VoConvert.string(row, "categoryCode"));
        vo.setCategoryName(VoConvert.string(row, "categoryName"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setScene(VoConvert.string(row, "scene"));
        vo.setAreaCode(VoConvert.string(row, "areaCode"));
        vo.setAreaName(VoConvert.string(row, "areaName"));
        vo.setProvince(VoConvert.string(row, "province"));
        vo.setCity(VoConvert.string(row, "city"));
        vo.setAddress(VoConvert.string(row, "address"));
        vo.setLocationText(VoConvert.string(row, "locationText"));
        vo.setDescription(VoConvert.string(row, "description"));
        vo.setImageUrl(VoConvert.string(row, "imageUrl"));
        vo.setLongitude(VoConvert.decimal(row, "longitude"));
        vo.setLatitude(VoConvert.decimal(row, "latitude"));
        vo.setMapX(VoConvert.intValue(row, "mapX"));
        vo.setMapY(VoConvert.intValue(row, "mapY"));
        vo.setRating(VoConvert.decimal(row, "rating"));
        vo.setHotness(VoConvert.intValue(row, "hotness"));
        vo.setVisitCount(VoConvert.intValue(row, "visitCount"));
        vo.setRouteEdgeCount(VoConvert.intValue(row, "routeEdgeCount"));
        vo.setStatus(VoConvert.intValue(row, "status"));
        return vo;
    }
}
