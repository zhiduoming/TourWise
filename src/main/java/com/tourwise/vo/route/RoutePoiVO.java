package com.tourwise.vo.route;

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
public class RoutePoiVO {
    private Long id;
    private String name;
    private String category;
    private String type;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer mapX;
    private Integer mapY;
    private Long placeGroupId;

    public static RoutePoiVO from(Map<String, Object> row) {
        RoutePoiVO vo = new RoutePoiVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setCategory(VoConvert.string(row, "category"));
        vo.setType(VoConvert.string(row, "type"));
        vo.setLongitude(VoConvert.decimal(row, "longitude"));
        vo.setLatitude(VoConvert.decimal(row, "latitude"));
        vo.setMapX(VoConvert.intValue(row, "mapX"));
        vo.setMapY(VoConvert.intValue(row, "mapY"));
        vo.setPlaceGroupId(VoConvert.longValue(row, "placeGroupId"));
        return vo;
    }
}
