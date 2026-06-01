package com.tourwise.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminSpotCreateRequest extends AdminSpotUpdateRequest {
    private Long id;
    private Long placeGroupId;
    private Long representativePoiId;
}
