package com.tourwise.vo.route;

import lombok.Data;

@Data
public class RouteGraphDiffEdgeVO {
    private String fromName;
    private String toName;
    private String description;
    private String changeType;
    private Integer fromMapX;
    private Integer fromMapY;
    private Integer toMapX;
    private Integer toMapY;
}
