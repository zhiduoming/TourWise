package com.tourwise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteMap {
    private Long id;
    private Long placeGroupId;
    private String imageUrl;
    private String originalName;
    private Integer mapWidth;
    private Integer mapHeight;
    private Long uploadedBy;
    private String createdAt;
    private String updatedAt;
}
