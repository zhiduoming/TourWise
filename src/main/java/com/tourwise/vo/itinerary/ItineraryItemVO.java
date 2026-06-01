package com.tourwise.vo.itinerary;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryItemVO {
    private Integer orderNo;
    private String itemType;
    private String timeSlot;
    private Long targetId;
    private Long spotId;
    private Long placeGroupId;
    private String name;
    private String description;
    private String address;
    private String image;
    private BigDecimal rating;
    private Integer hotness;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String recommendReason;
}
