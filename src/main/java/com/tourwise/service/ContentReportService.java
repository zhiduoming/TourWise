package com.tourwise.service;

import com.tourwise.common.BusinessException;
import com.tourwise.common.MapUtil;
import com.tourwise.common.PageResult;
import com.tourwise.dto.AdminReportHandleRequest;
import com.tourwise.dto.ReportCreateRequest;
import com.tourwise.mapper.ContentReportMapper;
import com.tourwise.mapper.LogMapper;
import com.tourwise.model.ContentReportRecord;
import com.tourwise.security.AuthContext;
import com.tourwise.vo.admin.ContentReportVO;
import com.tourwise.vo.common.ActionResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
public class ContentReportService {
    private static final Set<String> TARGET_TYPES = Set.of("log", "comment");
    private static final Set<String> HANDLE_STATUSES = Set.of("handled", "rejected");

    private final ContentReportMapper contentReportMapper;
    private final LogMapper logMapper;
    private final AdminService adminService;

    public ContentReportService(ContentReportMapper contentReportMapper, LogMapper logMapper, AdminService adminService) {
        this.contentReportMapper = contentReportMapper;
        this.logMapper = logMapper;
        this.adminService = adminService;
    }

    @Transactional
    public ActionResultVO create(ReportCreateRequest request) {
        long reporterId = AuthContext.requireUserId();
        String targetType = normalizeTargetType(request.getTargetType());
        Long targetId = normalizeId(request.getTargetId());
        if (targetId == null) {
            throw BusinessException.badRequest("举报对象不合法");
        }
        ensureTargetExists(targetType, targetId);
        if (contentReportMapper.existsPendingReport(targetType, targetId, reporterId) > 0) {
            throw BusinessException.badRequest("你已经举报过该内容，等待管理员处理即可");
        }
        ContentReportRecord record = new ContentReportRecord();
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        record.setReporterId(reporterId);
        record.setReason(request.getReason().trim());
        record.setDetail(trimToNull(request.getDetail()));
        contentReportMapper.insert(record);
        return ActionResultVO.created("reportId", record.getId());
    }

    public PageResult<ContentReportVO> list(String status, String targetType, String keyword, int page, int pageSize) {
        adminService.requireAdmin();
        String normalizedStatus = normalizeListStatus(status);
        String normalizedTargetType = normalizeOptionalTargetType(targetType);
        String normalizedKeyword = trimToNull(keyword);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;
        return new PageResult<>(
                contentReportMapper.list(normalizedStatus, normalizedTargetType, normalizedKeyword, offset, safePageSize)
                        .stream()
                        .map(MapUtil::normalize)
                        .map(ContentReportVO::from)
                        .toList(),
                contentReportMapper.count(normalizedStatus, normalizedTargetType, normalizedKeyword));
    }

    @Transactional
    public ContentReportVO handle(Long reportId, AdminReportHandleRequest request) {
        long handlerId = adminService.requireAdmin();
        ContentReportRecord report = contentReportMapper.findById(reportId);
        if (report == null) {
            throw BusinessException.notFound("举报记录不存在");
        }
        String status = normalizeHandleStatus(request.getStatus());
        if (Boolean.TRUE.equals(request.getDeleteTarget())) {
            deleteTarget(report);
        }
        ContentReportRecord update = new ContentReportRecord();
        update.setId(reportId);
        update.setStatus(status);
        update.setHandlerId(handlerId);
        update.setHandleNote(trimToNull(request.getHandleNote()));
        contentReportMapper.handle(update);
        return ContentReportVO.fromRecord(contentReportMapper.findById(reportId));
    }

    private void deleteTarget(ContentReportRecord report) {
        if ("log".equals(report.getTargetType())) {
            Long poiId = logMapper.findPoiId(report.getTargetId());
            logMapper.softDeleteByAdmin(report.getTargetId());
            if (poiId != null) {
                logMapper.refreshPoiRatingFromLogs(poiId);
                logMapper.refreshSpotRatingByPoiIdFromLogs(poiId);
            }
        } else if ("comment".equals(report.getTargetType())) {
            Long logId = logMapper.findCommentLogId(report.getTargetId());
            if (logMapper.softDeleteCommentByAdmin(report.getTargetId()) > 0 && logId != null) {
                logMapper.adjustHotness(logId, -1);
            }
        }
    }

    private void ensureTargetExists(String targetType, Long targetId) {
        if ("log".equals(targetType) && logMapper.exists(targetId) == 0) {
            throw BusinessException.notFound("被举报日志不存在");
        }
        if ("comment".equals(targetType) && logMapper.existsComment(targetId) == 0) {
            throw BusinessException.notFound("被举报评论不存在");
        }
    }

    private String normalizeTargetType(String targetType) {
        String value = trimToNull(targetType);
        if (!TARGET_TYPES.contains(value)) {
            throw BusinessException.badRequest("举报对象类型不合法");
        }
        return value;
    }

    private String normalizeOptionalTargetType(String targetType) {
        String value = trimToNull(targetType);
        if (value == null) {
            return null;
        }
        return normalizeTargetType(value);
    }

    private String normalizeListStatus(String status) {
        String value = trimToNull(status);
        if (value == null || "all".equals(value)) {
            return null;
        }
        if (!Set.of("pending", "handled", "rejected").contains(value)) {
            return "pending";
        }
        return value;
    }

    private String normalizeHandleStatus(String status) {
        String value = trimToNull(status);
        if (!HANDLE_STATUSES.contains(value)) {
            throw BusinessException.badRequest("处理状态不合法");
        }
        return value;
    }

    private Long normalizeId(Long id) {
        return id == null || id <= 0 ? null : id;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
