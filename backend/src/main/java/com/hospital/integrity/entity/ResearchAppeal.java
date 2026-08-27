package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 申诉表
 */
@Data
@TableName("research_appeal")
public class ResearchAppeal {

    /** 申诉ID */
    @TableId(type = IdType.AUTO)
    private Long appealId;

    /** 申诉编号，如 AP20260101001 */
    private String appealNo;

    /** 申诉人用户ID */
    private Long userId;

    /** 申诉对象：SCORE评价结果 DEDUCT扣分 CONFIRM认定结论 */
    private String appealType;

    /** 目标类型：INTEGRITY/VIOLATION/CHECK */
    private String bizType;

    /** 目标ID */
    private Long bizId;

    /** 申诉理由 */
    private String reason;

    /** 状态：PENDING/REVIEWING/SUSTAINED/OVERTURNED/REJECTED */
    private String status;

    /** 复核裁定结果 */
    private String result;

    /** 裁定人（诚信委员会） */
    private Long resultBy;

    /** 裁定时间 */
    private LocalDateTime resultTime;

    /** 提交时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
