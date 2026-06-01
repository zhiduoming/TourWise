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
public class ProfileVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String signature;
    private String phone;
    private String email;
    private String gender;
    private String birthday;
    private String createdAt;
    private String lastLoginAt;
    private String role;
    private Integer visits;
    private Integer favorites;

    public static ProfileVO from(Map<String, Object> row) {
        ProfileVO vo = new ProfileVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setUsername(VoConvert.string(row, "username"));
        vo.setNickname(VoConvert.string(row, "nickname"));
        vo.setAvatar(VoConvert.string(row, "avatar"));
        vo.setSignature(VoConvert.string(row, "signature"));
        vo.setPhone(VoConvert.string(row, "phone"));
        vo.setEmail(VoConvert.string(row, "email"));
        vo.setGender(VoConvert.string(row, "gender"));
        vo.setBirthday(VoConvert.string(row, "birthday"));
        vo.setCreatedAt(VoConvert.string(row, "createdAt"));
        vo.setLastLoginAt(VoConvert.string(row, "lastLoginAt"));
        vo.setRole(VoConvert.string(row, "role"));
        vo.setVisits(VoConvert.intValue(row, "visits"));
        vo.setFavorites(VoConvert.intValue(row, "favorites"));
        return vo;
    }
}
