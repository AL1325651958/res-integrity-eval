package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 诚信等级配置表
 */
@Data
@TableName("research_level_config")
public class ResearchLevelConfig {

    /** 配置ID */
    @TableId(type = IdType.AUTO)
    private Long levelId;

    /** 等级：A/B/C/D */
    private String level;

    /** 等级名称 */
    private String levelName;

    /** 有效扣分下限（含） */
    private BigDecimal minDeduct;

    /** 有效扣分上限（含） */
    private BigDecimal maxDeduct;

    /** 是否一票否决直接判定：1是 0否 */
    private Integer vetoFlag;

    /** 附加判定条件说明 */
    private String conditions;

    /** 版本 */
    private String version;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
