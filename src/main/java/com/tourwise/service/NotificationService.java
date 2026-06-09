package com.tourwise.service;

import com.tourwise.common.MapUtil;
import com.tourwise.common.PageResult;
import com.tourwise.mapper.NotificationMapper;
import com.tourwise.model.NotificationRecord;
import com.tourwise.security.AuthContext;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.user.NotificationVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class NotificationService {
    public static final String TYPE_LOG_LIKE = "log_like";
    public static final String TYPE_LOG_COMMENT = "log_comment";
    public static final String TYPE_ITINERARY_COPY = "itinerary_copy";
    public static final String TYPE_SYSTEM_WELCOME = "system_welcome";

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    public void notifyLogLike(Long receiverId, Long actorUserId, Long logId, String logTitle) {
        createIfNeeded(
                receiverId,
                actorUserId,
                TYPE_LOG_LIKE,
                "你的日志收到了新的点赞",
                "有人点赞了「" + displayTitle(logTitle, "这篇日志") + "」。",
                "log",
                logId,
                "/log/" + logId);
    }

    public void notifyLogComment(Long receiverId, Long actorUserId, Long logId, String logTitle, String content) {
        createIfNeeded(
                receiverId,
                actorUserId,
                TYPE_LOG_COMMENT,
                "你的日志收到了新的评论",
                "有人评论了「" + displayTitle(logTitle, "这篇日志") + "」：" + abbreviate(content, 60),
                "log",
                logId,
                "/log/" + logId);
    }

    public void notifyItineraryCopied(Long receiverId, Long actorUserId, Long planId, String planTitle) {
        createIfNeeded(
                receiverId,
                actorUserId,
                TYPE_ITINERARY_COPY,
                "你的行程被复制了",
                "有人复制了「" + displayTitle(planTitle, "这份行程") + "」。",
                "itinerary",
                planId,
                "/itinerary?sharedPlanId=" + planId);
    }

    public void notifyWelcome(Long receiverId, String username) {
        if (receiverId == null) {
            return;
        }
        String displayName = StringUtils.hasText(username) ? username.trim() : "旅行家";
        NotificationRecord record = new NotificationRecord();
        record.setUserId(receiverId);
        record.setActorUserId(null);
        record.setType(TYPE_SYSTEM_WELCOME);
        record.setTitle("欢迎加入 TourWise");
        record.setContent("Hi " + displayName + "，欢迎来到 TourWise！可以先浏览热门推荐，规划一份属于你的行程。");
        record.setTargetType("system");
        record.setTargetId(null);
        record.setLinkUrl("/recommend");
        notificationMapper.insert(record);
    }

    public PageResult<NotificationVO> list(Boolean onlyUnread, int page, int pageSize) {
        long userId = AuthContext.requireUserId();
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;
        List<NotificationVO> list = notificationMapper.list(userId, onlyUnread, offset, safePageSize)
                .stream()
                .map(MapUtil::normalize)
                .map(NotificationVO::from)
                .toList();
        return new PageResult<>(list, notificationMapper.count(userId, onlyUnread));
    }

    public int unreadCount() {
        return notificationMapper.countUnread(AuthContext.requireUserId());
    }

    public ActionResultVO markRead(Long id) {
        notificationMapper.markRead(id, AuthContext.requireUserId());
        return ActionResultVO.updated();
    }

    public ActionResultVO markAllRead() {
        notificationMapper.markAllRead(AuthContext.requireUserId());
        return ActionResultVO.updated();
    }

    private void createIfNeeded(
            Long receiverId,
            Long actorUserId,
            String type,
            String title,
            String content,
            String targetType,
            Long targetId,
            String linkUrl) {
        if (receiverId == null || actorUserId == null || receiverId.equals(actorUserId)) {
            return;
        }
        NotificationRecord record = new NotificationRecord();
        record.setUserId(receiverId);
        record.setActorUserId(actorUserId);
        record.setType(type);
        record.setTitle(title);
        record.setContent(content);
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        record.setLinkUrl(linkUrl);
        notificationMapper.insert(record);
    }

    private String displayTitle(String value, String fallback) {
        return StringUtils.hasText(value) ? abbreviate(value.trim(), 40) : fallback;
    }

    private String abbreviate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }
}
