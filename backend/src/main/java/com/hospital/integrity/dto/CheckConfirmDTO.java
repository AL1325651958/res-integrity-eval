package com.hospital.integrity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工单认定请求
 */
@Data
public class CheckConfirmDTO {

    /** 违规类型（字典 violation_type） */
    private String violationType;

    /** 违规等级 B/C/D */
    private String violationLevel;

    /** 扣分（不填则取规则默认） */
    private BigDecimal deductScore;

    private String description;

    private String evidence;

    /** 整改期限（B/C 级） */
    private LocalDate reformDeadline;
}
