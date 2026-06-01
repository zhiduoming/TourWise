package com.tourwise.vo.circle;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tourwise.vo.VoConvert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CircleVO {
    private Long id;
    private String name;
    private String description;
    private String cover;
    private Long ownerId;
    private String ownerName;
    private Integer status;
    private String createdAt;
    private Integer members;
    private Integer posts;
    private Integer logs;
    private Integer isMember;
    private List<CircleMemberVO> memberList;

    public static CircleVO from(Map<String, Object> row) {
        CircleVO vo = new CircleVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setName(VoConvert.string(row, "name"));
        vo.setDescription(VoConvert.string(row, "description"));
        vo.setCover(VoConvert.string(row, "cover"));
        vo.setOwnerId(firstLong(row, "ownerId", "owner_id"));
        vo.setOwnerName(VoConvert.string(row, "ownerName"));
        vo.setStatus(VoConvert.intValue(row, "status"));
        vo.setCreatedAt(firstString(row, "createdAt", "created_at"));
        vo.setMembers(VoConvert.intValue(row, "members"));
        vo.setPosts(VoConvert.intValue(row, "posts"));
        vo.setLogs(VoConvert.intValue(row, "logs"));
        vo.setIsMember(firstInt(row, "isMember", "is_member"));
        return vo;
    }

    @JsonProperty("owner_id")
    public Long getOwnerIdAlias() {
        return ownerId;
    }

    @JsonProperty("created_at")
    public String getCreatedAtAlias() {
        return createdAt;
    }

    @JsonProperty("is_member")
    public Integer getIsMemberAlias() {
        return isMember;
    }

    @JsonProperty("member_list")
    public List<CircleMemberVO> getMemberListAlias() {
        return memberList;
    }

    private static Long firstLong(Map<String, Object> row, String first, String second) {
        Long firstValue = VoConvert.longValue(row, first);
        return firstValue != null ? firstValue : VoConvert.longValue(row, second);
    }

    private static Integer firstInt(Map<String, Object> row, String first, String second) {
        Integer firstValue = VoConvert.intValue(row, first);
        return firstValue != null ? firstValue : VoConvert.intValue(row, second);
    }

    private static String firstString(Map<String, Object> row, String first, String second) {
        String firstValue = VoConvert.string(row, first);
        return firstValue != null ? firstValue : VoConvert.string(row, second);
    }
}
