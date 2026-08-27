package com.hospital.integrity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工单核查记录请求
 */
@Data
public class CheckRecordDTO {

    /** EVIDENCE取证 / INVESTIGATE调查 / CONFIRM认定 / HANDLE处置 / SIGN会签 */
    @NotBlank(message = "记录类型不能为空")
    private String recordType;

    private String content;

    public static CheckRecordDTO of(String recordType, String content) {
        CheckRecordDTO dto = new CheckRecordDTO();
        dto.setRecordType(recordType);
        dto.setContent(content);
        return dto;
    }
}
