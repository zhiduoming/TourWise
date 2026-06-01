package com.tourwise.vo.route;

import com.tourwise.vo.VoConvert;
import lombok.Data;

import java.util.Map;

@Data
public class RouteRecordPointVO {
    private Long id;
    private Long poiId;
    private String pointName;
    private Integer sortOrder;
    private Integer distanceFromStartM;
    private String description;

    public static RouteRecordPointVO from(Map<String, Object> row) {
        RouteRecordPointVO vo = new RouteRecordPointVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setPoiId(VoConvert.longValue(row, "poiId"));
        vo.setPointName(VoConvert.string(row, "pointName"));
        vo.setSortOrder(VoConvert.intValue(row, "sortOrder"));
        vo.setDistanceFromStartM(VoConvert.intValue(row, "distanceFromStartM"));
        vo.setDescription(VoConvert.string(row, "description"));
        return vo;
    }
}
