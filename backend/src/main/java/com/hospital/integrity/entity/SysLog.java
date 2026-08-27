package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表
 */
@Data
@TableName("sys_log")
public class SysLog {

    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Long logId;

    /** 操作人ID */
    private Long userId;

    /** 操作人账号 */
    private String username;

    /** 模块（成果/评价/工单/申诉/系统） */
    private String module;

    /** 操作描述 */
    private String operation;

    /** 请求方法 */
    private String method;

    /** 请求参数 */
    private String params;

    /** IP地址 */
    private String ip;

    /** 状态：1成功 0失败 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 耗时(ms) */
    private Long costTime;

    /** 操作时间 */
    private LocalDateTime createTime;
}
