package com.tourwise.vo.user;

import com.tourwise.vo.VoConvert;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class PreferenceSourceVO {
    private String source;
    private String sourceLabel;
    private BigDecimal weight;
    private Integer count;
    private String updatedAt;

    public static PreferenceSourceVO from(Map<String, Object> row) {
        PreferenceSourceVO vo = new PreferenceSourceVO();
        String source = VoConvert.string(row, "source");
        vo.setSource(source);
        vo.setSourceLabel(label(source));
        vo.setWeight(VoConvert.decimal(row, "weight"));
        vo.setCount(VoConvert.intValue(row, "count"));
        vo.setUpdatedAt(VoConvert.string(row, "updatedAt"));
        return vo;
    }

    private static String label(String source) {
        if ("browse".equals(source)) {
            return "浏览";
        }
        if ("favorite".equals(source)) {
            return "收藏";
        }
        if ("want".equals(source)) {
            return "想去";
        }
        if ("visited".equals(source)) {
            return "去过";
        }
        if ("dislike".equals(source)) {
            return "不感兴趣";
        }
        if ("rating".equals(source)) {
            return "评分";
        }
        if ("circle".equals(source)) {
            return "圈子";
        }
        if ("like".equals(source)) {
            return "点赞";
        }
        if ("comment".equals(source)) {
            return "评论";
        }
        return "手动";
    }
}
