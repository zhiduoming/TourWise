package com.tourwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCircleUpdateRequest {
    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Size(max = 255)
    private String description;

    @Size(max = 255)
    private String cover;
}
