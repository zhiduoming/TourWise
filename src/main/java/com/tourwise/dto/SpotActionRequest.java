package com.tourwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SpotActionRequest {
    @NotBlank(message = "对象类型不能为空")
    private String targetType;

    @NotNull(message = "对象ID不能为空")
    private Long targetId;
}
