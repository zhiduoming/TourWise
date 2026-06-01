package com.tourwise.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminSpotTagsRequest {
    private List<String> tags;
}
