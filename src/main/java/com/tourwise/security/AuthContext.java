package com.tourwise.security;

public final class AuthContext {
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setUserId(Long userId) {
        CURRENT_USER.set(userId);
    }

    public static Long getUserId() {
        return CURRENT_USER.get();
    }

    public static long requireUserId() {
        Long userId = CURRENT_USER.get();
        if (userId == null) {
            throw com.tourwise.common.BusinessException.unauthorized("未登录或登录已过期");
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
