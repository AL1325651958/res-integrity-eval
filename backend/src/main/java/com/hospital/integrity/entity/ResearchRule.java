package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评分规则表
 */
@Data
@TableName("research_rule")
public class ResearchRule {

    /** 规则ID */
    @TableId(type = IdType.AUTO)
    private Long ruleId;

    /** 规则类型：PERF业绩 DEDUCT扣分 */
    private String ruleType;

    /** 规则编号（唯一），如 PERF-PAPER-01 */
    private String ruleNo;

    /** 成果类型/违规类型（字典） */
    private String achType;

    /** 规则名称 */
    private String ruleName;

    /** 基础分 */
    private BigDecimal baseScore;

    /** 计算模式：FIXED/COEFF/ROLE_MAP/MONEY */
    private String calcMode;

    /** 配置：ROLE_MAP分值映射 或 MONEY{unit_amount,score_per_unit,cap} */
    private String configJson;

    /** 规则版本 */
    private String version;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期（NULL为长期有效） */
    private LocalDate expireDate;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 是否一票否决：1是 0否（扣分规则） */
    private Integer vetoFlag;

    /** 是否需整改：1是 0否（B/C级） */
    private Integer needReform;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
