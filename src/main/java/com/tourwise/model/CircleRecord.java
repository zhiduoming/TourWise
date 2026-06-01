package com.tourwise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CircleRecord {
    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private Long ownerId;
}
