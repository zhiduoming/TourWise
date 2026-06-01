package com.tourwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReportHandleRequest {
    @NotBlank(message = "处理状态不能为空")
    private String status;

    private Boolean deleteTarget;

    @Size(max = 1000, message = "处理备注过长")
    private String handleNote;
}
