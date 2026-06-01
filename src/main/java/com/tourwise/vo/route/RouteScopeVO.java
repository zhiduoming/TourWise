package com.tourwise.vo.route;

import com.tourwise.vo.VoConvert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteScopeVO {
    private Long placeGroupId;
    private String name;
    private Integer poiCount;
    private String routeGraphStatus;
    private Integer mapWidth;
    private Integer mapHeight;

    public static RouteScopeVO from(Map<String, Object> row) {
        RouteScopeVO vo = new RouteScopeVO();
        vo.setPlaceGroupId(VoConvert.longValue(row, "placeGroupId"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setPoiCount(VoConvert.intValue(row, "poiCount"));
        vo.setRouteGraphStatus(VoConvert.string(row, "routeGraphStatus"));
        vo.setMapWidth(VoConvert.intValue(row, "mapWidth"));
        vo.setMapHeight(VoConvert.intValue(row, "mapHeight"));
        return vo;
    }
}
