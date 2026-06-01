package com.tourwise.mapper;

import com.tourwise.model.RouteRecord;
import com.tourwise.model.RouteRecordPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RouteRecordMapper {
    int insertRecord(RouteRecord record);

    int insertPoint(RouteRecordPoint point);

    List<Map<String, Object>> listRecords(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    Map<String, Object> findRecordById(@Param("id") Long id, @Param("userId") Long userId);

    long countRecords(@Param("userId") Long userId);

    List<Map<String, Object>> listPoints(@Param("recordId") Long recordId);

    int deleteRecord(@Param("id") Long id, @Param("userId") Long userId);
}
