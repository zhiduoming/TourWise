package com.tourwise.service.route;

import java.util.List;

public class RouteSearchOptions {
    private final boolean fastest;
    private final boolean avoidCrowd;

    public RouteSearchOptions(List<String> preferences) {
        this.fastest = preferences != null && preferences.contains("fastest");
        this.avoidCrowd = preferences != null && preferences.contains("avoid_crowd");
    }

    public boolean isFastest() {
        return fastest;
    }

    public boolean isAvoidCrowd() {
        return avoidCrowd;
    }
}
