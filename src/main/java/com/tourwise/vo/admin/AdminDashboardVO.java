package com.tourwise.vo.admin;

import lombok.Data;

import java.util.List;

@Data
public class AdminDashboardVO {
    private List<AdminMetricVO> metrics;
    private List<DataQualityVO> dataQuality;
    private List<RouteGraphQualityVO> routeQuality;
    private Integer issueCount;
    private Integer errorCount;
    private Integer warningCount;
}
