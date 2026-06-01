package com.tourwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportCreateRequest {
    @NotBlank(message = "举报对象类型不能为空")
    private String targetType;

    @NotNull(message = "举报对象不能为空")
    private Long targetId;

    @NotBlank(message = "举报原因不能为空")
    @Size(max = 100, message = "举报原因过长")
    private String reason;

    @Size(max = 1000, message = "补充说明过长")
    private String detail;
}
