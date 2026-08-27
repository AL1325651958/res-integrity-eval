package com.hospital.integrity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 整改提交/验收请求
 */
@Data
public class ReformDTO {

    @NotBlank(message = "整改情况不能为空")
    private String result;

    /** 验收是否通过（reformCheck 使用） */
    private Boolean pass;

    private String comment;
}
