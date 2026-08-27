package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 失信核查工单表
 */
@Data
@TableName("research_check")
public class ResearchCheck {

    /** 工单ID */
    @TableId(type = IdType.AUTO)
    private Long checkId;

    /** 工单号，如 CK20260101001 */
    private String checkNo;

    /** 被核查人用户ID */
    private Long userId;

    /** 关联成果ID */
    private Long achId;

    /** 来源：AUTO自动筛查 MANUAL人工 */
    private String riskSource;

    /** 风险类型（字典 risk_type） */
    private String riskType;

    /** 风险描述 */
    private String riskDesc;

    /** 来源预警记录ID */
    private Long riskLogId;

    /** 状态：PENDING/PROCESSING/TO_CONFIRM/CONFIRMED/TO_PUBLIC/PUBLISHED/ARCHIVED/DISMISSED */
    private String status;

    /** 认领人（科研科审核员） */
    private Long assignee;

    /** 认领时间 */
    private LocalDateTime claimTime;

    /** 调查结论 */
    private String conclusion;

    /** 认定扣分 */
    private BigDecimal deductScore;

    /** 认定后生成的违规记录ID */
    private Long violationId;

    /** 处置结果说明 */
    private String handleResult;

    /** 关联公示记录ID */
    private Long publicityId;

    /** 办结时间 */
    private LocalDateTime finishTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
