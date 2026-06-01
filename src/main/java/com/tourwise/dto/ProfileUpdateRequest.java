package com.tourwise.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {
    @Size(max = 50)
    private String nickname;

    @Size(max = 120)
    private String signature;

    private String gender;
    private LocalDate birthday;
}
