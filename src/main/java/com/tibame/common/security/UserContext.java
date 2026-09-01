package com.tibame.common.security;

public final class UserContext {

    private static final ThreadLocal<UserPrincipal> USER_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUser(UserPrincipal userPrincipal) {
        USER_HOLDER.set(userPrincipal);
    }

    public static UserPrincipal getUser() {
        return USER_HOLDER.get();
    }

    public static Long getUserId() {
        UserPrincipal user = USER_HOLDER.get();
        return user != null ? user.getId() : null;
    }

    public static Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new com.tibame.common.exception.UnauthorizedException("用戶尚未登入或 Token 已失效");
        }
        return userId;
    }

    public static String getUsername() {
        UserPrincipal user = USER_HOLDER.get();
        return user != null ? user.getUsername() : null;
    }

    public static void clear() {
        USER_HOLDER.remove();
    }
}
