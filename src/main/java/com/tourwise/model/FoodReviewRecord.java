package com.tourwise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodReviewRecord {
    private Long id;
    private Long foodId;
    private Long userId;
    private BigDecimal rating;
    private String content;
}
