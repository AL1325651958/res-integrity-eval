package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志表
 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {

    /** 登录日志ID */
    @TableId(type = IdType.AUTO)
    private Long loginId;

    /** 登录账号 */
    private String username;

    /** IP地址 */
    private String ip;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 状态：1成功 0失败 */
    private Integer status;

    /** 提示信息 */
    private String msg;

    /** 登录时间 */
    private LocalDateTime loginTime;
}
