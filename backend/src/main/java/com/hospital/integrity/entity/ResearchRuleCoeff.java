package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评分系数表
 */
@Data
@TableName("research_rule_coeff")
public class ResearchRuleCoeff {

    /** 系数ID */
    @TableId(type = IdType.AUTO)
    private Long coeffId;

    /** 规则ID（NULL=该系数类型通用） */
    private Long ruleId;

    /** 系数类型：RANK作者位次 TOPIC_ROLE课题角色 REWARD_RANK获奖排名 */
    private String coeffType;

    /** 档位键：1ST/CORRESP/2ND/3RD/4TH/5TH+ 或 LEADER/CORE/MEMBER */
    private String positionKey;

    /** 档位说明，如 第一作者/通讯作者 */
    private String positionLabel;

    /** 系数值 */
    private BigDecimal coefficient;

    /** 排序 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createTime;
}
