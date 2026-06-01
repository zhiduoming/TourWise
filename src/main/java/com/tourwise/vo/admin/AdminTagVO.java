package com.tourwise.vo.admin;

import com.tourwise.vo.VoConvert;
import lombok.Data;

import java.util.Map;

@Data
public class AdminTagVO {
    private Long id;
    private String name;
    private String tagType;

    public static AdminTagVO from(Map<String, Object> row) {
        AdminTagVO vo = new AdminTagVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setTagType(VoConvert.string(row, "tagType"));
        return vo;
    }
}
