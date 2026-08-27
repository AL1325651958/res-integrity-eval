package com.hospital.integrity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 成果审核请求
 */
@Data
public class AuditDTO {

    /** APPROVE 通过 / BACK 退回 */
    @NotBlank(message = "审核类型不能为空")
    private String auditType;

    private String opinion;
}
