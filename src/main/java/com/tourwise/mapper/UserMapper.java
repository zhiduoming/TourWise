package com.tourwise.mapper;

import com.tourwise.model.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    UserAccount findAccountByUsernameOrPhone(@Param("account") String account);

    int existsByUsername(@Param("username") String username);

    int existsByPhone(@Param("phone") String phone);

    int insertUser(UserAccount account);

    int insertProfile(@Param("userId") Long userId, @Param("nickname") String nickname, @Param("avatar") String avatar);

    int updateLastLoginAt(@Param("id") Long id);

    Map<String, Object> findUserSummary(@Param("id") Long id);

    Map<String, Object> findPublicUser(@Param("id") Long id);

    Map<String, Object> findProfile(@Param("id") Long id);

    List<Map<String, Object>> listPreferenceTags(@Param("userId") Long userId, @Param("limit") int limit);

    List<Map<String, Object>> listPreferenceSources(@Param("userId") Long userId);

    Map<String, Object> preferenceSummary(@Param("userId") Long userId);

    String findRoleById(@Param("id") Long id);

    int updateProfile(
            @Param("userId") Long userId,
            @Param("nickname") String nickname,
            @Param("signature") String signature,
            @Param("gender") String gender,
            @Param("birthday") LocalDate birthday);

    int updateAvatar(@Param("userId") Long userId, @Param("avatar") String avatar);
}
