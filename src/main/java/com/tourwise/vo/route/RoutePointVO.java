package com.tourwise.vo.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoutePointVO extends RoutePoiVO {
    private String description;
    private Integer distance;

    public RoutePointVO(RoutePoiVO poi, String description, Integer distance) {
        super(
                poi.getId(),
                poi.getName(),
                poi.getCategory(),
                poi.getType(),
                poi.getLongitude(),
                poi.getLatitude(),
                poi.getMapX(),
                poi.getMapY(),
                poi.getPlaceGroupId());
        this.description = description;
        this.distance = distance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }
}
