package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 科研成果主表
 */
@Data
@TableName("research_achievement")
public class ResearchAchievement {

    /** 成果ID */
    @TableId(type = IdType.AUTO)
    private Long achId;

    /** 申报人用户ID */
    private Long userId;

    /** 成果类型（字典 ach_type） */
    private String achType;

    /** 成果标题/名称 */
    private String title;

    /** 编号：DOI/专利号/项目号/证书号 */
    private String achNo;

    /** 来源：期刊名/立项部门/颁奖单位 */
    private String sourceName;

    /** 发表/立项/授权时间 */
    private LocalDateTime publishTime;

    /** 级别：期刊分区/项目级别/奖励级别（如 SCI-1区、国家级） */
    private String level;

    /** 位次/角色：如 第1作者、负责人 */
    private String rankInfo;

    /** 是否通讯作者：1是 0否（仅论文） */
    private Integer isCorresponding;

    /** 经费/到账金额（元） */
    private BigDecimal fundAmount;

    /** 系统计算得分 */
    private BigDecimal score;

    /** 计分状态：0未计分 1已计分 2已作废回收 */
    private Integer scoreStatus;

    /** 审核状态：0草稿 1待科室初审 2待科研科终审 3已入库 4已退回 5已撤销 6已作废 */
    private Integer status;

    /** 最近审核人ID */
    private Long auditBy;

    /** 最近审核时间 */
    private LocalDateTime auditTime;

    /** 审核意见 */
    private String auditRemark;

    /** 风险标记：0正常 1风险预警中 2已处置 */
    private Integer riskFlag;

    /** 创建人 */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新人 */
    private Long updateBy;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
