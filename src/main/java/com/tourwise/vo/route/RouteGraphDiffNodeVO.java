package com.tourwise.vo.route;

import lombok.Data;

@Data
public class RouteGraphDiffNodeVO {
    private String name;
    private String nodeType;
    private String changeType;
    private Integer currentMapX;
    private Integer currentMapY;
    private Integer versionMapX;
    private Integer versionMapY;
}
