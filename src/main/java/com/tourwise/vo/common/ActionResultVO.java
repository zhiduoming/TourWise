package com.tourwise.vo.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActionResultVO {
    private Boolean created;
    private Boolean updated;
    private Boolean deleted;
    private Boolean joined;
    private Boolean left;
    private Boolean liked;
    private Boolean rated;
    private Boolean logout;
    private Long id;
    private Long userId;
    private Long logId;
    private Long circleId;
    private Long commentId;
    private Long reviewId;
    private String username;

    public static ActionResultVO created(String idName, Long id) {
        ActionResultVO vo = new ActionResultVO();
        vo.setCreated(true);
        setNamedId(vo, idName, id);
        return vo;
    }

    public static ActionResultVO updated() {
        ActionResultVO vo = new ActionResultVO();
        vo.setUpdated(true);
        return vo;
    }

    public static ActionResultVO deleted() {
        ActionResultVO vo = new ActionResultVO();
        vo.setDeleted(true);
        return vo;
    }

    public static ActionResultVO joined() {
        ActionResultVO vo = new ActionResultVO();
        vo.setJoined(true);
        return vo;
    }

    public static ActionResultVO left() {
        ActionResultVO vo = new ActionResultVO();
        vo.setLeft(true);
        return vo;
    }

    public static ActionResultVO liked(boolean liked) {
        ActionResultVO vo = new ActionResultVO();
        vo.setLiked(liked);
        return vo;
    }

    public static ActionResultVO rated() {
        ActionResultVO vo = new ActionResultVO();
        vo.setRated(true);
        return vo;
    }

    public static ActionResultVO logout() {
        ActionResultVO vo = new ActionResultVO();
        vo.setLogout(true);
        return vo;
    }

    private static void setNamedId(ActionResultVO vo, String idName, Long id) {
        switch (idName) {
            case "userId" -> vo.setUserId(id);
            case "logId" -> vo.setLogId(id);
            case "circleId" -> vo.setCircleId(id);
            case "commentId" -> vo.setCommentId(id);
            case "reviewId" -> vo.setReviewId(id);
            default -> vo.setId(id);
        }
    }
}
