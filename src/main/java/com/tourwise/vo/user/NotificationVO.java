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
public class NotificationVO {
    private Long id;
    private Long userId;
    private Long actorUserId;
    private String actorName;
    private String actorAvatar;
    private String type;
    private String title;
    private String content;
    private String targetType;
    private Long targetId;
    private String linkUrl;
    private Boolean read;
    private String createdAt;

    public static NotificationVO from(Map<String, Object> row) {
        NotificationVO vo = new NotificationVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setUserId(VoConvert.longValue(row, "userId"));
        vo.setActorUserId(VoConvert.longValue(row, "actorUserId"));
        vo.setActorName(VoConvert.string(row, "actorName"));
        vo.setActorAvatar(VoConvert.string(row, "actorAvatar"));
        vo.setType(VoConvert.string(row, "type"));
        vo.setTitle(VoConvert.string(row, "title"));
        vo.setContent(VoConvert.string(row, "content"));
        vo.setTargetType(VoConvert.string(row, "targetType"));
        vo.setTargetId(VoConvert.longValue(row, "targetId"));
        vo.setLinkUrl(VoConvert.string(row, "linkUrl"));
        vo.setRead(Boolean.TRUE.equals(VoConvert.bool(row, "isRead")));
        vo.setCreatedAt(VoConvert.string(row, "createdAt"));
        return vo;
    }
}
