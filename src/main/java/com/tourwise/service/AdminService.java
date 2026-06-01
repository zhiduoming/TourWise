package com.tourwise.service;

import com.tourwise.common.BusinessException;
import com.tourwise.mapper.UserMapper;
import com.tourwise.security.AuthContext;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    private static final String ADMIN_ROLE = "admin";

    private final UserMapper userMapper;

    public AdminService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public long requireAdmin() {
        long userId = AuthContext.requireUserId();
        if (!isAdmin(userId)) {
            throw BusinessException.forbidden("仅管理员可以执行该操作");
        }
        return userId;
    }

    public boolean isAdmin(long userId) {
        String role = userMapper.findRoleById(userId);
        return ADMIN_ROLE.equalsIgnoreCase(role);
    }
}
