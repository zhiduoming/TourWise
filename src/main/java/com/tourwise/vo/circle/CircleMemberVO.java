package com.tourwise.vo.circle;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tourwise.vo.VoConvert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CircleMemberVO {
    private Long userId;
    private Integer role;
    private String joinedAt;
    private String username;
    private String avatar;

    public static CircleMemberVO from(Map<String, Object> row) {
        CircleMemberVO vo = new CircleMemberVO();
        vo.setUserId(firstLong(row, "userId", "user_id"));
        vo.setRole(VoConvert.intValue(row, "role"));
        vo.setJoinedAt(firstString(row, "joinedAt", "joined_at"));
        vo.setUsername(VoConvert.string(row, "username"));
        vo.setAvatar(VoConvert.string(row, "avatar"));
        return vo;
    }

    @JsonProperty("user_id")
    public Long getUserIdAlias() {
        return userId;
    }

    @JsonProperty("joined_at")
    public String getJoinedAtAlias() {
        return joinedAt;
    }

    private static Long firstLong(Map<String, Object> row, String first, String second) {
        Long firstValue = VoConvert.longValue(row, first);
        return firstValue != null ? firstValue : VoConvert.longValue(row, second);
    }

    private static String firstString(Map<String, Object> row, String first, String second) {
        String firstValue = VoConvert.string(row, first);
        return firstValue != null ? firstValue : VoConvert.string(row, second);
    }
}
