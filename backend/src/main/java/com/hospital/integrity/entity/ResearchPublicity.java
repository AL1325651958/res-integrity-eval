package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公示记录表
 */
@Data
@TableName("research_publicity")
public class ResearchPublicity {

    /** 公示ID */
    @TableId(type = IdType.AUTO)
    private Long publicityId;

    /** 公示编号 */
    private String publicityNo;

    /** 类型：EVALUATE评价结果公示 CONFIRM失信认定公示 */
    private String publicityType;

    /** 关联对象类型：INTEGRITY/CHECK */
    private String bizType;

    /** 关联对象ID列表 */
    private String bizIds;

    /** 公示范围：ALL全院 DEPT科室 OWN个人 */
    private String scope;

    /** 公示开始时间 */
    private LocalDateTime startTime;

    /** 公示结束时间 */
    private LocalDateTime endTime;

    /** 状态：PUBLISHING/FINISHED/CANCELED */
    private String status;

    /** 创建人 */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}
