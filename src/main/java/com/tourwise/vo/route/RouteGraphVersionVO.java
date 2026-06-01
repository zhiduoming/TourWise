package com.tourwise.vo.route;

import com.tourwise.model.RouteGraphVersionRecord;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RouteGraphVersionVO {
    private Long id;
    private Long placeGroupId;
    private Integer versionNo;
    private String name;
    private Integer nodeCount;
    private Integer edgeCount;
    private Long createdBy;
    private String createdByName;
    private String remark;
    private LocalDateTime createdAt;

    public static RouteGraphVersionVO from(RouteGraphVersionRecord record) {
        RouteGraphVersionVO vo = new RouteGraphVersionVO();
        vo.setId(record.getId());
        vo.setPlaceGroupId(record.getPlaceGroupId());
        vo.setVersionNo(record.getVersionNo());
        vo.setName(record.getName());
        vo.setNodeCount(record.getNodeCount());
        vo.setEdgeCount(record.getEdgeCount());
        vo.setCreatedBy(record.getCreatedBy());
        vo.setCreatedByName(record.getCreatedByName());
        vo.setRemark(record.getRemark());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
