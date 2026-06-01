package com.tourwise.mapper;

import com.tourwise.model.NotificationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface NotificationMapper {
    int insert(NotificationRecord record);

    List<Map<String, Object>> list(
            @Param("userId") Long userId,
            @Param("onlyUnread") Boolean onlyUnread,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long count(
            @Param("userId") Long userId,
            @Param("onlyUnread") Boolean onlyUnread);

    int countUnread(@Param("userId") Long userId);

    int markRead(
            @Param("id") Long id,
            @Param("userId") Long userId);

    int markAllRead(@Param("userId") Long userId);
}
