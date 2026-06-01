package com.tourwise.vo.route;

import com.tourwise.model.RouteGraphEdgeRecord;
import lombok.Data;

@Data
public class RouteGraphEdgeVO {
    private Long id;
    private Long fromPoiId;
    private Long toPoiId;
    private Integer distanceM;
    private Integer durationMin;
    private String transportType;
    private Boolean indoor;
    private String description;

    public static RouteGraphEdgeVO from(RouteGraphEdgeRecord record) {
        RouteGraphEdgeVO vo = new RouteGraphEdgeVO();
        vo.setId(record.getId());
        vo.setFromPoiId(record.getFromPoiId());
        vo.setToPoiId(record.getToPoiId());
        vo.setDistanceM(record.getDistanceM());
        vo.setDurationMin(record.getDurationMin());
        vo.setTransportType(record.getTransportType());
        vo.setIndoor(Boolean.TRUE.equals(record.getIndoor()));
        vo.setDescription(record.getDescription());
        return vo;
    }
}
