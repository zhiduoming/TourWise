package com.tourwise.dto;

import lombok.Data;

import java.util.List;

@Data
public class ItineraryDaySaveRequest {
    private Integer dayNo;
    private String title;
    private String summary;
    private List<ItineraryItemSaveRequest> items;
}
