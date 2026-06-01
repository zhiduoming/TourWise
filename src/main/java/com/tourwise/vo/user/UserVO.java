package com.tourwise.vo.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tourwise.vo.VoConvert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String signature;
    private String phone;
    private String role;
    private Integer visits;

    public static UserVO from(Map<String, Object> row) {
        UserVO vo = new UserVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setUsername(VoConvert.string(row, "username"));
        vo.setNickname(VoConvert.string(row, "nickname"));
        vo.setAvatar(VoConvert.string(row, "avatar"));
        vo.setSignature(VoConvert.string(row, "signature"));
        vo.setPhone(VoConvert.string(row, "phone"));
        vo.setRole(VoConvert.string(row, "role"));
        vo.setVisits(VoConvert.intValue(row, "visits"));
        return vo;
    }
}
