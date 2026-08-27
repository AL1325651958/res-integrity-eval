package com.hospital.integrity.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图（含角色）
 */
@Data
public class UserVO {

    private Long userId;
    private String empNo;
    private String username;
    private String realName;
    private Long deptId;
    private String deptName;
    private String title;
    private String phone;
    private String email;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private List<Long> roleIds;
    private String roleNames;
}
