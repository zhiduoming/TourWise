package com.tourwise.mapper;

import com.tourwise.model.*;
import com.tourwise.dto.AdminCircleUpdateRequest;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CircleMapper {
    List<Map<String, Object>> list(@Param("currentUserId") Long currentUserId, @Param("keyword") String keyword);

    List<Map<String, Object>> adminList(
            @Param("currentUserId") Long currentUserId,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long adminCount(@Param("keyword") String keyword, @Param("status") Integer status);

    Map<String, Object> findById(@Param("id") Long id, @Param("currentUserId") Long currentUserId);

    Map<String, Object> adminFindById(@Param("currentUserId") Long currentUserId, @Param("id") Long id);

    List<Map<String, Object>> listMembers(@Param("circleId") Long circleId);

    int exists(@Param("id") Long id);

    int existsAny(@Param("id") Long id);

    Long ownerId(@Param("id") Long id);

    int isMember(@Param("circleId") Long circleId, @Param("userId") Long userId);

    int insert(CircleRecord record);

    int insertMember(@Param("circleId") Long circleId, @Param("userId") Long userId, @Param("role") int role);

    int deleteMember(@Param("circleId") Long circleId, @Param("userId") Long userId);

    int updateByAdmin(@Param("id") Long id, @Param("request") AdminCircleUpdateRequest request);

    int updateStatusByAdmin(@Param("id") Long id, @Param("status") int status);
}
