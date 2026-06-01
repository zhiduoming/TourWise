package com.tourwise.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMetricVO {
    private String label;
    private String key;
    private Integer value;
    private String description;
}
