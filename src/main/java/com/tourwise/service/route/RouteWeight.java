package com.tourwise.service.route;

import com.tourwise.model.RouteEdge;

import java.math.BigDecimal;

public final class RouteWeight {
    private RouteWeight() {
    }

    public static int cost(RouteEdge edge, RouteSearchOptions options) {
        BigDecimal factor = edge.getCongestionFactor() == null ? BigDecimal.ONE : edge.getCongestionFactor();
        if (options != null && options.isFastest()) {
            return Math.max(1, factor.multiply(BigDecimal.valueOf(edge.getDurationMin())).intValue());
        }
        if (options != null && options.isAvoidCrowd()) {
            return Math.max(1, factor.multiply(BigDecimal.valueOf(edge.getDistanceM())).intValue());
        }
        return Math.max(1, edge.getDistanceM());
    }
}
