package com.tourwise.dto;

import lombok.Data;

@Data
public class RouteGraphEdgeRequest {
    private String fromClientId;
    private String toClientId;
    private String description;
    private String transportType = "walk";
    private Boolean indoor = false;
    private Boolean bidirectional = true;
}
