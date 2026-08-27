package com.hospital.integrity.security;

import com.hospital.integrity.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException(401, "未登录或登录已过期");
    }

    public static Long currentUserId() {
        return currentUser().getUserId();
    }

    public static boolean hasRole(String roleKey) {
        return currentUser().getRoleKeys().contains(roleKey);
    }

    public static boolean isAdmin() {
        return hasRole("admin");
    }
}
