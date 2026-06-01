package com.tourwise.service;

import com.tourwise.dto.*;
import com.tourwise.mapper.*;
import com.tourwise.model.*;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.common.PageResult;
import com.tourwise.security.AuthContext;
import com.tourwise.vo.common.ActionResultVO;
import com.tourwise.vo.log.CommentVO;
import com.tourwise.vo.log.LogVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogService {
    private final LogMapper logMapper;
    private final AdminService adminService;
    private final CircleMapper circleMapper;
    private final ItineraryPlanMapper itineraryPlanMapper;
    private final NotificationService notificationService;

    public LogService(
            LogMapper logMapper,
            AdminService adminService,
            CircleMapper circleMapper,
            ItineraryPlanMapper itineraryPlanMapper,
            NotificationService notificationService) {
        this.logMapper = logMapper;
        this.adminService = adminService;
        this.circleMapper = circleMapper;
        this.itineraryPlanMapper = itineraryPlanMapper;
        this.notificationService = notificationService;
    }

    public PageResult<LogVO> list(
            Long circleId,
            Long userId,
            Long spotId,
            String keyword,
            String tab,
            int page,
            int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;
        Long currentUserId = AuthContext.getUserId();
        String normalizedKeyword = trimToNull(keyword);
        List<Map<String, Object>> rows = logMapper.list(
                currentUserId,
                normalizeId(circleId),
                normalizeId(userId),
                normalizeId(spotId),
                normalizedKeyword,
                trimToNull(tab),
                offset,
                safePageSize);
        List<LogVO> logs = attachImagesAndTags(rows).stream().map(LogVO::from).toList();
        long total = logMapper.count(normalizeId(circleId), normalizeId(userId), normalizeId(spotId), normalizedKeyword);
        return new PageResult<>(logs, total);
    }

    public PageResult<LogVO> myLogs(String keyword, String tab, int page, int pageSize) {
        long userId = AuthContext.requireUserId();
        return list(null, userId, null, keyword, tab, page, pageSize);
    }

    public PageResult<LogVO> adminLogs(
            Long circleId,
            Long userId,
            Long spotId,
            String keyword,
            String tab,
            int page,
            int pageSize) {
        adminService.requireAdmin();
        return list(circleId, userId, spotId, keyword, tab, page, pageSize);
    }

    public LogVO detail(Long id) {
        logMapper.increaseViewCount(id);
        Map<String, Object> row = logMapper.findById(id, AuthContext.getUserId());
        if (row == null) {
            throw BusinessException.notFound("日志不存在");
        }
        return LogVO.from(attachImagesAndTags(List.of(row)).get(0));
    }

    @Transactional
    public ActionResultVO create(LogCreateRequest request) {
        return createForCircle(request, normalizeId(request.getCircleId()));
    }

    @Transactional
    public ActionResultVO createForCircle(LogCreateRequest request, Long circleId) {
        long userId = AuthContext.requireUserId();
        LogRecord record = new LogRecord();
        record.setUserId(userId);
        record.setPoiId(firstNonNull(normalizeId(request.getSpotId()), normalizeId(request.getPoiId())));
        record.setCircleId(normalizeId(circleId));
        record.setItineraryPlanId(normalizePlanId(request.getItineraryPlanId(), userId));
        ensureCircleWritable(record.getCircleId(), userId);
        record.setTitle(trimToNull(request.getTitle()));
        record.setContent(request.getContent().trim());
        record.setSceneryRating(normalizeRating(request.getSceneryRating()));
        record.setFacilityRating(normalizeRating(request.getFacilityRating()));
        record.setServiceRating(normalizeRating(request.getServiceRating()));
        record.setTrafficRating(normalizeRating(request.getTrafficRating()));
        record.setValueRating(normalizeRating(request.getValueRating()));
        record.setRating(resolveOverallRating(request, record));
        logMapper.insert(record);
        insertImages(record.getId(), request.getImages());
        insertTags(record.getId(), buildTags(request.getTags(), record.getItineraryPlanId()));
        if (record.getPoiId() != null && record.getRating() != null) {
            logMapper.refreshPoiRatingFromLogs(record.getPoiId());
            logMapper.refreshSpotRatingByPoiIdFromLogs(record.getPoiId());
        }
        return ActionResultVO.created("logId", record.getId());
    }

    private void ensureCircleWritable(Long circleId, long userId) {
        if (circleId == null) {
            return;
        }
        if (circleMapper.exists(circleId) == 0) {
            throw BusinessException.notFound("圈子不存在");
        }
        if (circleMapper.isMember(circleId, userId) == 0) {
            throw BusinessException.unauthorized("加入圈子后才能把日志发布到该圈子");
        }
    }

    @Transactional
    public ActionResultVO delete(Long id) {
        long userId = AuthContext.requireUserId();
        if (logMapper.exists(id) == 0) {
            throw BusinessException.notFound("日志不存在");
        }
        Long ownerId = logMapper.findOwnerId(id);
        Long poiId = logMapper.findPoiId(id);
        if (ownerId != null && ownerId == userId) {
            logMapper.softDelete(id, userId);
            refreshRatingAfterDelete(poiId);
            return ActionResultVO.deleted();
        }
        if (adminService.isAdmin(userId)) {
            logMapper.softDeleteByAdmin(id);
            refreshRatingAfterDelete(poiId);
            return ActionResultVO.deleted();
        }
        if (logMapper.softDelete(id, userId) == 0) {
            throw BusinessException.unauthorized("只能删除自己发布的日志");
        }
        refreshRatingAfterDelete(poiId);
        return ActionResultVO.deleted();
    }

    @Transactional
    public ActionResultVO adminDelete(Long id) {
        adminService.requireAdmin();
        if (logMapper.exists(id) == 0) {
            throw BusinessException.notFound("日志不存在");
        }
        Long poiId = logMapper.findPoiId(id);
        logMapper.softDeleteByAdmin(id);
        refreshRatingAfterDelete(poiId);
        return ActionResultVO.deleted();
    }

    @Transactional
    public ActionResultVO toggleLike(Long id) {
        long userId = AuthContext.requireUserId();
        if (logMapper.exists(id) == 0) {
            throw BusinessException.notFound("日志不存在");
        }
        boolean liked;
        if (logMapper.hasLike(id, userId) > 0) {
            logMapper.deleteLike(id, userId);
            logMapper.adjustHotness(id, -2);
            liked = false;
        } else {
            logMapper.insertLike(id, userId);
            logMapper.adjustHotness(id, 2);
            Map<String, Object> notificationInfo = logMapper.findNotificationInfo(id);
            if (notificationInfo != null) {
                Map<String, Object> normalized = MapUtil.normalize(notificationInfo);
                notificationService.notifyLogLike(
                        getLong(normalized, "userId"),
                        userId,
                        id,
                        stringValue(normalized.get("title")));
            }
            liked = true;
        }
        return ActionResultVO.liked(liked);
    }

    public PageResult<CommentVO> comments(Long logId) {
        if (logMapper.exists(logId) == 0) {
            throw BusinessException.notFound("日志不存在");
        }
        List<Map<String, Object>> normalized = logMapper.listComments(logId)
                .stream()
                .map(LogService::normalizeLogMap)
                .toList();
        return new PageResult<>(nestComments(normalized), normalized.size());
    }

    public PageResult<CommentVO> adminComments(Long logId) {
        adminService.requireAdmin();
        return comments(logId);
    }

    @Transactional
    public ActionResultVO adminDeleteComment(Long commentId) {
        adminService.requireAdmin();
        Long logId = logMapper.findCommentLogId(commentId);
        if (logId == null) {
            throw BusinessException.notFound("评论不存在");
        }
        if (logMapper.softDeleteCommentByAdmin(commentId) == 0) {
            throw BusinessException.notFound("评论不存在");
        }
        logMapper.adjustHotness(logId, -1);
        return ActionResultVO.deleted();
    }

    @Transactional
    public ActionResultVO createComment(Long logId, CommentRequest request) {
        long userId = AuthContext.requireUserId();
        if (logMapper.exists(logId) == 0) {
            throw BusinessException.notFound("日志不存在");
        }
        Long parentId = normalizeId(request.getParentId());
        if (parentId != null && logMapper.existsCommentInLog(parentId, logId) == 0) {
            throw BusinessException.badRequest("父评论不存在");
        }
        CommentRecord record = new CommentRecord();
        record.setLogId(logId);
        record.setUserId(userId);
        record.setParentId(parentId);
        record.setContent(request.getContent().trim());
        logMapper.insertComment(record);
        logMapper.adjustHotness(logId, 1);
        Map<String, Object> notificationInfo = logMapper.findNotificationInfo(logId);
        if (notificationInfo != null) {
            Map<String, Object> normalized = MapUtil.normalize(notificationInfo);
            notificationService.notifyLogComment(
                    getLong(normalized, "userId"),
                    userId,
                    logId,
                    stringValue(normalized.get("title")),
                    record.getContent());
        }
        return ActionResultVO.created("commentId", record.getId());
    }

    private List<Map<String, Object>> attachImagesAndTags(List<Map<String, Object>> rows) {
        List<Map<String, Object>> logs = rows.stream().map(LogService::normalizeLogMap).toList();
        List<Long> logIds = logs.stream()
                .map(row -> ((Number) row.get("id")).longValue())
                .toList();
        if (logIds.isEmpty()) {
            return logs;
        }
        Map<Long, List<String>> imagesByLog = new LinkedHashMap<>();
        for (Map<String, Object> row : logMapper.listImages(logIds)) {
            Long logId = getLong(row, "log_id", "logId");
            imagesByLog.computeIfAbsent(logId, key -> new ArrayList<>())
                    .add((String) row.get("image_url"));
        }
        Map<Long, List<String>> tagsByLog = new LinkedHashMap<>();
        for (Map<String, Object> row : logMapper.listTags(logIds)) {
            Long logId = getLong(row, "log_id", "logId");
            tagsByLog.computeIfAbsent(logId, key -> new ArrayList<>())
                    .add((String) row.get("name"));
        }
        for (Map<String, Object> log : logs) {
            Long logId = ((Number) log.get("id")).longValue();
            log.put("images", imagesByLog.getOrDefault(logId, List.of()));
            log.put("tags", tagsByLog.getOrDefault(logId, List.of()));
        }
        return logs;
    }

    private void insertImages(Long logId, List<String> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        int order = 1;
        for (String image : images) {
            String normalized = trimToNull(image);
            if (normalized != null && normalized.length() <= 255) {
                logMapper.insertImage(logId, normalized, order++);
            }
        }
    }

    private void insertTags(Long logId, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (String tag : tags) {
            String normalized = trimToNull(tag);
            if (normalized == null || normalized.length() > 50) {
                continue;
            }
            logMapper.insertIgnoreTag(normalized);
            Long tagId = logMapper.findLogTagId(normalized);
            if (tagId != null) {
                logMapper.insertLogTag(logId, tagId);
            }
        }
    }

    private Long normalizePlanId(Long itineraryPlanId, long userId) {
        Long normalized = normalizeId(itineraryPlanId);
        if (normalized == null) {
            return null;
        }
        if (itineraryPlanMapper.findPlanById(normalized, userId) == null) {
            throw BusinessException.notFound("行程不存在或无权分享");
        }
        return normalized;
    }

    private List<String> buildTags(List<String> tags, Long itineraryPlanId) {
        List<String> result = new ArrayList<>();
        if (tags != null) {
            result.addAll(tags);
        }
        if (itineraryPlanId != null && result.stream().noneMatch("行程分享"::equals)) {
            result.add("行程分享");
        }
        return result;
    }

    private void refreshRatingAfterDelete(Long poiId) {
        if (poiId != null) {
            logMapper.refreshPoiRatingFromLogs(poiId);
            logMapper.refreshSpotRatingByPoiIdFromLogs(poiId);
        }
    }

    private static BigDecimal resolveOverallRating(LogCreateRequest request, LogRecord record) {
        BigDecimal explicitRating = normalizeRating(request.getRating());
        if (explicitRating != null) {
            return explicitRating;
        }
        List<BigDecimal> ratings = java.util.stream.Stream.of(
                record.getSceneryRating(),
                record.getFacilityRating(),
                record.getServiceRating(),
                record.getTrafficRating(),
                record.getValueRating()
        ).filter(value -> value != null).toList();
        if (ratings.isEmpty()) {
            return null;
        }
        BigDecimal total = ratings.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(ratings.size()), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeRating(BigDecimal rating) {
        if (rating == null) {
            return null;
        }
        if (rating.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (rating.compareTo(BigDecimal.valueOf(5)) > 0) {
            return BigDecimal.valueOf(5);
        }
        return rating.setScale(1, RoundingMode.HALF_UP);
    }

    private static List<CommentVO> nestComments(List<Map<String, Object>> comments) {
        Map<Long, CommentVO> topLevel = new LinkedHashMap<>();
        List<CommentVO> replies = new ArrayList<>();
        for (Map<String, Object> comment : comments) {
            CommentVO vo = CommentVO.from(comment);
            if (vo.getParentId() == null) {
                topLevel.put(vo.getId(), vo);
            } else {
                replies.add(vo);
            }
        }
        for (CommentVO reply : replies) {
            CommentVO parent = topLevel.get(reply.getParentId());
            if (parent == null) {
                topLevel.put(reply.getId(), reply);
            } else {
                parent.getReplies().add(reply);
            }
        }
        return new ArrayList<>(topLevel.values());
    }

    private static Map<String, Object> normalizeLogMap(Map<String, Object> row) {
        Map<String, Object> item = MapUtil.normalize(row);
        alias(item, "userId", "user_id");
        alias(item, "spotId", "spot_id");
        alias(item, "circleId", "circle_id");
        alias(item, "viewCount", "view_count");
        alias(item, "isTop", "is_top");
        alias(item, "createdAt", "created_at");
        alias(item, "likeCount", "like_count");
        alias(item, "commentCount", "comment_count");
        alias(item, "isLiked", "is_liked");
        alias(item, "logId", "log_id");
        alias(item, "parentId", "parent_id");
        alias(item, "sceneryRating", "scenery_rating");
        alias(item, "facilityRating", "facility_rating");
        alias(item, "serviceRating", "service_rating");
        alias(item, "trafficRating", "traffic_rating");
        alias(item, "valueRating", "value_rating");
        return item;
    }

    private static void alias(Map<String, Object> item, String camelKey, String snakeKey) {
        if (item.containsKey(camelKey)) {
            item.put(snakeKey, item.get(camelKey));
        }
    }

    private static Long getLong(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
        }
        throw new IllegalStateException("缺少必要ID字段");
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long normalizeId(Long id) {
        return id == null || id <= 0 ? null : id;
    }

    private static Long firstNonNull(Long first, Long second) {
        return first != null ? first : second;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
