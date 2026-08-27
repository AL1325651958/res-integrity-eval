package com.hospital.integrity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 申诉提交请求
 */
@Data
public class AppealDTO {

    /** SCORE评价结果 / DEDUCT扣分 / CONFIRM认定结论 */
    @NotBlank(message = "申诉对象不能为空")
    private String appealType;

    /** INTEGRITY / VIOLATION / CHECK */
    @NotBlank(message = "目标类型不能为空")
    private String bizType;

    @NotNull(message = "目标ID不能为空")
    private Long bizId;

    @NotBlank(message = "申诉理由不能为空")
    private String reason;
}
