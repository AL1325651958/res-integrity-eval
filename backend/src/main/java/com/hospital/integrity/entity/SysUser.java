package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long userId;

    /** 工号（唯一） */
    private String empNo;

    /** 登录账号（唯一） */
    private String username;

    /** 密码（BCrypt） */
    private String password;

    /** 姓名 */
    private String realName;

    /** 所属科室ID */
    private Long deptId;

    /** 职称 */
    private String title;

    /** 手机号（加密存储，脱敏展示） */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像地址 */
    private String avatar;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 删除标记：0正常 1已删除 */
    private Integer delFlag;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新人 */
    private String updateBy;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
