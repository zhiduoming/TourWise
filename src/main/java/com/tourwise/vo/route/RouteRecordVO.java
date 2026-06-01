package com.tourwise.vo.route;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tourwise.vo.VoConvert;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RouteRecordVO {
    private Long id;
    private String routeName;
    private String mode;
    private Integer totalDistanceM;
    private Integer totalDurationMin;
    private String createdAt;
    private Map<String, Object> metadata;
    private List<RouteRecordPointVO> points;

    @JsonIgnore
    private String preferencesRaw;

    public static RouteRecordVO from(Map<String, Object> row) {
        RouteRecordVO vo = new RouteRecordVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setRouteName(VoConvert.string(row, "routeName"));
        vo.setMode(VoConvert.string(row, "mode"));
        vo.setTotalDistanceM(VoConvert.intValue(row, "totalDistanceM"));
        vo.setTotalDurationMin(VoConvert.intValue(row, "totalDurationMin"));
        vo.setCreatedAt(VoConvert.string(row, "createdAt"));
        vo.setPreferencesRaw(VoConvert.string(row, "preferences"));
        return vo;
    }
}
