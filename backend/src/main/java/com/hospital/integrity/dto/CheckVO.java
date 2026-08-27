package com.hospital.integrity.dto;

import com.hospital.integrity.entity.ResearchCheck;
import com.hospital.integrity.entity.ResearchCheckRecord;
import com.hospital.integrity.entity.ResearchViolation;
import lombok.Data;

import java.util.List;

/**
 * 工单详情
 */
@Data
public class CheckVO {

    private ResearchCheck check;
    private List<ResearchCheckRecord> records;
    private ResearchViolation violation;
}
