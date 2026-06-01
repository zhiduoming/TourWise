package com.tourwise.vo.user;

import com.tourwise.vo.VoConvert;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class PreferenceTagVO {
    private Long tagId;
    private String tagName;
    private String tagType;
    private BigDecimal weight;
    private Integer sourceCount;
    private String sourceSummary;
    private String updatedAt;

    public static PreferenceTagVO from(Map<String, Object> row) {
        PreferenceTagVO vo = new PreferenceTagVO();
        vo.setTagId(VoConvert.longValue(row, "tagId"));
        vo.setTagName(VoConvert.string(row, "tagName"));
        vo.setTagType(VoConvert.string(row, "tagType"));
        vo.setWeight(VoConvert.decimal(row, "weight"));
        vo.setSourceCount(VoConvert.intValue(row, "sourceCount"));
        vo.setSourceSummary(VoConvert.string(row, "sourceSummary"));
        vo.setUpdatedAt(VoConvert.string(row, "updatedAt"));
        return vo;
    }
}
