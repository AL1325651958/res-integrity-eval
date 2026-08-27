package com.hospital.integrity.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户信息（登录返回/当前用户）
 */
@Data
public class UserInfoVO {

    private Long userId;
    private String username;
    private String realName;
    private Long deptId;
    private String deptName;
    private String title;
    private List<String> roles;
    private List<String> perms;
}
