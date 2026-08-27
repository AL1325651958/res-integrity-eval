package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 科研诚信年度评价表
 */
@Data
@TableName("research_integrity")
public class ResearchIntegrity {

    /** 评价ID */
    @TableId(type = IdType.AUTO)
    private Long integrityId;

    /** 用户ID */
    private Long userId;

    /** 评价年度 */
    private Integer year;

    /** 周期类型：YEAR年度 PERIOD周期 */
    private String periodType;

    /** 业绩总分 */
    private BigDecimal perfScore;

    /** 有效诚信扣分 */
    private BigDecimal deductScore;

    /** 最终得分 */
    private BigDecimal totalScore;

    /** 诚信等级：A/B/C/D */
    private String level;

    /** 是否一票否决：1是 0否 */
    private Integer vetoFlag;

    /** 使用的规则版本 */
    private String ruleVersion;

    /** 计算状态：0待计算 1已计算 2已重算 */
    private Integer calcStatus;

    /** 评价说明 */
    private String remark;

    /** 评定人ID */
    private Long assessor;

    /** 评定时间 */
    private LocalDateTime assessTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
