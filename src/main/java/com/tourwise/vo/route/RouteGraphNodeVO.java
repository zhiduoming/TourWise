package com.tourwise.vo.route;

import com.tourwise.model.RouteGraphNodeRecord;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RouteGraphNodeVO {
    private Long id;
    private String clientId;
    private String name;
    private String nodeType;
    private String categoryCode;
    private String categoryName;
    private String areaCode;
    private String areaName;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer mapX;
    private Integer mapY;
    private Boolean visible;

    public static RouteGraphNodeVO from(RouteGraphNodeRecord record) {
        RouteGraphNodeVO vo = new RouteGraphNodeVO();
        vo.setId(record.getId());
        vo.setClientId("id:" + record.getId());
        vo.setName(record.getName());
        vo.setCategoryCode(record.getCategoryCode());
        vo.setCategoryName(record.getCategoryName());
        vo.setAreaCode(record.getAreaCode());
        vo.setAreaName(record.getAreaName());
        vo.setLongitude(record.getLongitude());
        vo.setLatitude(record.getLatitude());
        vo.setMapX(record.getMapX());
        vo.setMapY(record.getMapY());
        boolean route = "route".equals(record.getAreaCode());
        vo.setNodeType(route ? "route" : "poi");
        vo.setVisible(!route);
        return vo;
    }
}
