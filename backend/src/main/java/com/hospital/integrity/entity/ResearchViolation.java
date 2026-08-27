package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 违规记录表
 */
@Data
@TableName("research_violation")
public class ResearchViolation {

    /** 违规记录ID */
    @TableId(type = IdType.AUTO)
    private Long violationId;

    /** 违规人用户ID */
    private Long userId;

    /** 关联成果ID */
    private Long achId;

    /** 来源核查工单ID */
    private Long checkId;

    /** 违规类型（字典 violation_type） */
    private String violationType;

    /** 违规等级：B/C/D */
    private String violationLevel;

    /** 认定扣分（一票否决记0，以veto_flag标识） */
    private BigDecimal deductScore;

    /** 违规描述 */
    private String description;

    /** 证据说明 */
    private String evidence;

    /** 状态：CONFIRMED/EFFECTIVE/REFORMING/REFORMED/REVOKED */
    private String status;

    /** 扣分生效日期 */
    private LocalDate effectiveDate;

    /** 是否一票否决：1是 0否 */
    private Integer vetoFlag;

    /** 整改期限 */
    private LocalDate reformDeadline;

    /** 整改情况说明 */
    private String reformResult;

    /** 整改验收人ID */
    private Long reformCheckBy;

    /** 整改验收时间 */
    private LocalDateTime reformCheckTime;

    /** 创建人 */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
