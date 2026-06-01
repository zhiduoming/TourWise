package com.tourwise.vo.route;

import lombok.Data;

@Data
public class RouteGraphIssueVO {
    private String type;
    private String severity;
    private String title;
    private String description;
    private Long nodeId;
    private String nodeName;
    private Long fromNodeId;
    private String fromNodeName;
    private Long toNodeId;
    private String toNodeName;
    private Long edgeId;
    private Integer distanceM;
}
