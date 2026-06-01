package com.tourwise.vo.circle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CircleListVO {
    private List<CircleVO> joinedCircles;
    private List<CircleVO> otherCircles;
    private List<CircleVO> list;
    private Integer total;
}
