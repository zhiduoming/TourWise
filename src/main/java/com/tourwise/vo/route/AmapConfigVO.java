package com.tourwise.vo.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmapConfigVO {
    private Boolean enabled;
    private String jsKey;
    private String securityCode;
    private String message;
}
