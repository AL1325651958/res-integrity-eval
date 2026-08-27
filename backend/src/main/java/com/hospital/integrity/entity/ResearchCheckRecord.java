package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 核查记录表
 */
@Data
@TableName("research_check_record")
public class ResearchCheckRecord {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long recordId;

    /** 工单ID */
    private Long checkId;

    /** 类型：EVIDENCE取证 INVESTIGATE调查 CONFIRM认定 HANDLE处置 SIGN会签 */
    private String recordType;

    /** 记录内容 */
    private String content;

    /** 操作人ID */
    private Long operator;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作时间 */
    private LocalDateTime operateTime;
}
