package com.tourwise.vo.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tourwise.model.RouteMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteMapVO {
    private Long placeGroupId;
    private String imageUrl;
    private String originalName;
    private Integer mapWidth;
    private Integer mapHeight;
    private Long uploadedBy;
    private String updatedAt;

    public static RouteMapVO from(RouteMap routeMap) {
        if (routeMap == null) {
            return null;
        }
        RouteMapVO vo = new RouteMapVO();
        vo.setPlaceGroupId(routeMap.getPlaceGroupId());
        vo.setImageUrl(routeMap.getImageUrl());
        vo.setOriginalName(routeMap.getOriginalName());
        vo.setMapWidth(routeMap.getMapWidth());
        vo.setMapHeight(routeMap.getMapHeight());
        vo.setUploadedBy(routeMap.getUploadedBy());
        vo.setUpdatedAt(routeMap.getUpdatedAt());
        return vo;
    }
}
