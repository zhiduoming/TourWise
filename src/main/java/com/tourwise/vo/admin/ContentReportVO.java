package com.tourwise.vo.admin;

import com.tourwise.model.ContentReportRecord;
import lombok.Data;

import java.util.Map;

@Data
public class ContentReportVO {
    private Long id;
    private String targetType;
    private Long targetId;
    private Long reporterId;
    private String reporterName;
    private String targetTitle;
    private String targetContent;
    private String reason;
    private String detail;
    private String status;
    private Long handlerId;
    private String handlerName;
    private String handleNote;
    private String handledAt;
    private String createdAt;
    private String updatedAt;

    public static ContentReportVO from(Map<String, Object> row) {
        ContentReportVO vo = new ContentReportVO();
        vo.setId(longValue(row.get("id")));
        vo.setTargetType(string(row.get("targetType")));
        vo.setTargetId(longValue(row.get("targetId")));
        vo.setReporterId(longValue(row.get("reporterId")));
        vo.setReporterName(string(row.get("reporterName")));
        vo.setTargetTitle(string(row.get("targetTitle")));
        vo.setTargetContent(string(row.get("targetContent")));
        vo.setReason(string(row.get("reason")));
        vo.setDetail(string(row.get("detail")));
        vo.setStatus(string(row.get("status")));
        vo.setHandlerId(longValue(row.get("handlerId")));
        vo.setHandlerName(string(row.get("handlerName")));
        vo.setHandleNote(string(row.get("handleNote")));
        vo.setHandledAt(string(row.get("handledAt")));
        vo.setCreatedAt(string(row.get("createdAt")));
        vo.setUpdatedAt(string(row.get("updatedAt")));
        return vo;
    }

    public static ContentReportVO fromRecord(ContentReportRecord record) {
        ContentReportVO vo = new ContentReportVO();
        vo.setId(record.getId());
        vo.setTargetType(record.getTargetType());
        vo.setTargetId(record.getTargetId());
        vo.setReporterId(record.getReporterId());
        vo.setReason(record.getReason());
        vo.setDetail(record.getDetail());
        vo.setStatus(record.getStatus());
        vo.setHandlerId(record.getHandlerId());
        vo.setHandleNote(record.getHandleNote());
        vo.setHandledAt(string(record.getHandledAt()));
        vo.setCreatedAt(string(record.getCreatedAt()));
        vo.setUpdatedAt(string(record.getUpdatedAt()));
        return vo;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

}
