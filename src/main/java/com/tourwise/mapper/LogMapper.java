package com.tourwise.mapper;

import com.tourwise.model.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface LogMapper {
    List<Map<String, Object>> list(
            @Param("currentUserId") Long currentUserId,
            @Param("circleId") Long circleId,
            @Param("userId") Long userId,
            @Param("spotId") Long spotId,
            @Param("foodId") Long foodId,
            @Param("keyword") String keyword,
            @Param("tab") String tab,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long count(
            @Param("circleId") Long circleId,
            @Param("userId") Long userId,
            @Param("spotId") Long spotId,
            @Param("foodId") Long foodId,
            @Param("keyword") String keyword);

    Map<String, Object> findById(@Param("id") Long id, @Param("currentUserId") Long currentUserId);

    int increaseViewCount(@Param("id") Long id);

    int insert(LogRecord record);

    int insertImage(@Param("logId") Long logId, @Param("imageUrl") String imageUrl, @Param("sortOrder") int sortOrder);

    int insertIgnoreTag(@Param("name") String name);

    Long findLogTagId(@Param("name") String name);

    int insertLogTag(@Param("logId") Long logId, @Param("tagId") Long tagId);

    List<Map<String, Object>> listImages(@Param("logIds") List<Long> logIds);

    List<Map<String, Object>> listTags(@Param("logIds") List<Long> logIds);

    int exists(@Param("id") Long id);

    Long findOwnerId(@Param("id") Long id);

    Map<String, Object> findNotificationInfo(@Param("id") Long id);

    Long findPoiId(@Param("id") Long id);

    int softDelete(@Param("id") Long id, @Param("userId") Long userId);

    int softDeleteByAdmin(@Param("id") Long id);

    int refreshPoiRatingFromLogs(@Param("poiId") Long poiId);

    int refreshSpotRatingByPoiIdFromLogs(@Param("poiId") Long poiId);

    int hasLike(@Param("logId") Long logId, @Param("userId") Long userId);

    int insertLike(@Param("logId") Long logId, @Param("userId") Long userId);

    int deleteLike(@Param("logId") Long logId, @Param("userId") Long userId);

    int adjustHotness(@Param("id") Long id, @Param("delta") int delta);

    int insertComment(CommentRecord record);

    int existsCommentInLog(@Param("id") Long id, @Param("logId") Long logId);

    int existsComment(@Param("id") Long id);

    Long findCommentLogId(@Param("id") Long id);

    int softDeleteCommentByAdmin(@Param("id") Long id);

    List<Map<String, Object>> listComments(@Param("logId") Long logId);
}
