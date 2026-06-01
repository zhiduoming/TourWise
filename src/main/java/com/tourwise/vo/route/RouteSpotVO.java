package com.tourwise.vo.route;

import com.tourwise.vo.VoConvert;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class RouteSpotVO {
    private Long id;
    private Long placeGroupId;
    private String name;
    private String shortName;
    private String spotType;
    private String province;
    private String city;
    private String district;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer locationRadiusM;
    private String routeGraphStatus;

    public static RouteSpotVO from(Map<String, Object> row) {
        RouteSpotVO vo = new RouteSpotVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setPlaceGroupId(VoConvert.longValue(row, "placeGroupId"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setShortName(VoConvert.string(row, "shortName"));
        vo.setSpotType(VoConvert.string(row, "spotType"));
        vo.setProvince(VoConvert.string(row, "province"));
        vo.setCity(VoConvert.string(row, "city"));
        vo.setDistrict(VoConvert.string(row, "district"));
        vo.setLongitude(VoConvert.decimal(row, "longitude"));
        vo.setLatitude(VoConvert.decimal(row, "latitude"));
        vo.setLocationRadiusM(VoConvert.intValue(row, "locationRadiusM"));
        vo.setRouteGraphStatus(VoConvert.string(row, "routeGraphStatus"));
        return vo;
    }
}
