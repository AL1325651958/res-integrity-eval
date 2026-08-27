package com.hospital.integrity.dto;

import com.hospital.integrity.entity.ResearchAchievement;
import com.hospital.integrity.entity.ResearchAttachment;
import com.hospital.integrity.entity.ResearchAuditLog;
import lombok.Data;

import java.util.List;

/**
 * 成果详情（含附件与审核流水）
 */
@Data
public class AchievementVO {

    private ResearchAchievement achievement;
    private List<ResearchAttachment> attachments;
    private List<ResearchAuditLog> auditLogs;
}
