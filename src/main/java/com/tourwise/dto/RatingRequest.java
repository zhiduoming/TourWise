package com.tourwise.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingRequest {
    @NotBlank
    private String targetType;

    @NotNull
    private Long targetId;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private BigDecimal rating;
}
