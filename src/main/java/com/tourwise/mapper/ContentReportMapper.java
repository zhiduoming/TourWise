package com.tourwise.mapper;

import com.tourwise.model.ContentReportRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ContentReportMapper {
    int existsPendingReport(
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("reporterId") Long reporterId);

    int insert(ContentReportRecord record);

    List<Map<String, Object>> list(
            @Param("status") String status,
            @Param("targetType") String targetType,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long count(
            @Param("status") String status,
            @Param("targetType") String targetType,
            @Param("keyword") String keyword);

    ContentReportRecord findById(@Param("id") Long id);

    int handle(ContentReportRecord record);
}
