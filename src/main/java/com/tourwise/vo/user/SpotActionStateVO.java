package com.tourwise.vo.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpotActionStateVO {
    private String targetType;
    private Long targetId;
    private Boolean favorite;
    private Boolean wantToGo;
    private Boolean visited;
    private Boolean disliked;
}
