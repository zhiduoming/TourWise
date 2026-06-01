package com.tourwise.vo.itinerary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryFavoriteStateVO {
    private Long planId;
    private Boolean favorited;
    private Integer favoriteCount;
}
