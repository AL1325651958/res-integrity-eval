package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评价明细快照表
 */
@Data
@TableName("research_integrity_detail")
public class ResearchIntegrityDetail {

    /** 明细ID */
    @TableId(type = IdType.AUTO)
    private Long detailId;

    /** 评价ID */
    private Long integrityId;

    /** 用户ID */
    private Long userId;

    /** 评价年度 */
    private Integer year;

    /** 类型：PERF业绩 DEDUCT扣分 */
    private String bizType;

    /** 关联成果ID（业绩明细） */
    private Long achId;

    /** 关联违规记录ID（扣分明细） */
    private Long violationId;

    /** 规则ID */
    private Long ruleId;

    /** 规则版本 */
    private String ruleVersion;

    /** 计分项名称，如 SCI一区·第一作者 */
    private String itemName;

    /** 基础分 */
    private BigDecimal baseScore;

    /** 系数 */
    private BigDecimal coefficient;

    /** 得分/扣分（扣分为负数） */
    private BigDecimal score;

    /** 规则快照（当时分值配置） */
    private String snapshot;

    /** 创建时间 */
    private LocalDateTime createTime;
}
