package com.tourwise.vo.user;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PreferenceProfileVO {
    private BigDecimal totalWeight;
    private Integer tagCount;
    private Integer signalCount;
    private String lastUpdatedAt;
    private String summary;
    private String recommendationTip;
    private List<PreferenceTagVO> topTags;
    private List<PreferenceSourceVO> sources;
}
