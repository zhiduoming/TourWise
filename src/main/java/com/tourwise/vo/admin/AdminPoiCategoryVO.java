package com.tourwise.vo.admin;

import com.tourwise.vo.VoConvert;
import lombok.Data;

import java.util.Map;

@Data
public class AdminPoiCategoryVO {
    private Long id;
    private String code;
    private String name;
    private String scene;

    public static AdminPoiCategoryVO from(Map<String, Object> row) {
        AdminPoiCategoryVO vo = new AdminPoiCategoryVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setCode(VoConvert.string(row, "code"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setScene(VoConvert.string(row, "scene"));
        return vo;
    }
}
