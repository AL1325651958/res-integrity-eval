package com.hospital.integrity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成果审核流水表
 */
@Data
@TableName("research_audit_log")
public class ResearchAuditLog {

    /** 审核流水ID */
    @TableId(type = IdType.AUTO)
    private Long auditId;

    /** 成果ID */
    private Long achId;

    /** 操作类型：SUBMIT提交 BACK退回 APPROVE终审通过 CANCEL撤销 INVALID作废 */
    private String auditType;

    /** 操作人ID */
    private Long auditBy;

    /** 操作人姓名 */
    private String auditName;

    /** 审核意见 */
    private String opinion;

    /** 操作时间 */
    private LocalDateTime auditTime;
}
