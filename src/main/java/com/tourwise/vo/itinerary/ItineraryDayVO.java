package com.tourwise.vo.itinerary;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryDayVO {
    private Integer dayNo;
    private String title;
    private String summary;
    private Integer estimatedDistanceM;
    private List<ItineraryItemVO> items = new ArrayList<>();
}
