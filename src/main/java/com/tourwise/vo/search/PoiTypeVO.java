package com.tourwise.vo.search;

import com.tourwise.vo.VoConvert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiTypeVO {
    private String label;
    private String value;
    private String scene;

    public static PoiTypeVO from(Map<String, Object> row) {
        return new PoiTypeVO(
                VoConvert.string(row, "label"),
                VoConvert.string(row, "value"),
                VoConvert.string(row, "scene"));
    }
}
