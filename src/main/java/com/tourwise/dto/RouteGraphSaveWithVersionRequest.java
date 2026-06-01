package com.tourwise.dto;

import lombok.Data;

@Data
public class RouteGraphSaveWithVersionRequest {
    private RouteGraphSaveRequest graph;
    private Boolean createVersion = true;
    private String versionName;
    private String versionRemark;
}
