package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风险筛查记录表
 */
@Data
@TableName("research_risk_log")
public class ResearchRiskLog {

    /** 预警记录ID */
    @TableId(type = IdType.AUTO)
    private Long riskId;

    /** 涉事用户ID */
    private Long userId;

    /** 关联成果ID */
    private Long achId;

    /** 风险类型（字典 risk_type） */
    private String riskType;

    /** 命中筛查规则 */
    private String ruleNo;

    /** 风险描述 */
    private String riskDesc;

    /** 命中依据：DOI/标题/时间等 */
    private String matchValue;

    /** 状态：NEW新增 CLAIMED已认领 DISMISSED误报 CONFIRMED确认 */
    private String status;

    /** 转成的工单ID */
    private Long checkId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
