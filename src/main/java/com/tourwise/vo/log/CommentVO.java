package com.tourwise.vo.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tourwise.vo.VoConvert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentVO {
    private Long id;
    private Long logId;
    private Long userId;
    private Long parentId;
    private String username;
    private String avatar;
    private String content;
    private String createdAt;
    private List<CommentVO> replies = new ArrayList<>();

    public static CommentVO from(Map<String, Object> row) {
        CommentVO vo = new CommentVO();
        vo.setId(VoConvert.longValue(row, "id"));
        vo.setLogId(firstLong(row, "logId", "log_id"));
        vo.setUserId(firstLong(row, "userId", "user_id"));
        vo.setParentId(firstLong(row, "parentId", "parent_id"));
        vo.setUsername(VoConvert.string(row, "username"));
        vo.setAvatar(VoConvert.string(row, "avatar"));
        vo.setContent(VoConvert.string(row, "content"));
        vo.setCreatedAt(firstString(row, "createdAt", "created_at"));
        vo.setReplies(new ArrayList<>());
        return vo;
    }

    @JsonProperty("log_id")
    public Long getLogIdAlias() {
        return logId;
    }

    @JsonProperty("user_id")
    public Long getUserIdAlias() {
        return userId;
    }

    @JsonProperty("parent_id")
    public Long getParentIdAlias() {
        return parentId;
    }

    @JsonProperty("created_at")
    public String getCreatedAtAlias() {
        return createdAt;
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
