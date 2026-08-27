package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 黑名单表
 */
@Data
@TableName("research_blacklist")
public class ResearchBlacklist {

    /** 黑名单ID */
    @TableId(type = IdType.AUTO)
    private Long blId;

    /** 类型：JOURNAL期刊 PUBLISHER出版社 KEYWORD关键词 */
    private String blType;

    /** 名称/关键词 */
    private String blName;

    /** 来源：中科院预警名单2025/自定义 */
    private String source;

    /** 风险等级：高/中/低 */
    private String riskLevel;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
