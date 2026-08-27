package com.hospital.integrity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 申诉复核裁定请求
 */
@Data
public class AppealReviewDTO {

    /** SUSTAINED维持原判 / OVERTURNED变更裁定 / REJECTED驳回 */
    @NotBlank(message = "裁定结果不能为空")
    private String pass;

    private String result;
}
