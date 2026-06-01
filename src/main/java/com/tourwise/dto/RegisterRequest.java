package com.tourwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;

    @NotBlank
    @Size(min = 6, max = 50)
    private String password;

    @NotBlank
    private String confirmPassword;
}
