package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.dto.CheckConfirmDTO;
import com.hospital.integrity.dto.CheckRecordDTO;
import com.hospital.integrity.dto.CheckVO;
import com.hospital.integrity.entity.*;
import com.hospital.integrity.mapper.*;
import com.hospital.integrity.security.LoginUser;
import com.hospital.integrity.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 失信核查工单全流程：认领 → 取证调查 → 认定 → 公示 → 生效 → 归档；违规整改与验收。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckService {

    private final ResearchCheckMapper checkMapper;
    private final ResearchCheckRecordMapper recordMapper;
    private final ResearchRiskLogMapper riskLogMapper;
    private final ResearchViolationMapper violationMapper;
    private final ScoreEngine scoreEngine;
    private final IntegrityService integrityService;
    private final NoticeService noticeService;
    private final PublicityService publicityService;

    // ---------------- 预警 → 工单 ----------------

    @Transactional
    public ResearchCheck claimFromLog(Long riskLogId) {
        requireOperator();
        ResearchRiskLog riskLog = riskLogMapper.selectById(riskLogId);
        if (riskLog == null) {
            throw new BusinessException("预警记录不存在");
        }
        if (riskLog.getCheckId() != null) {
            throw new BusinessException("该预警已转工单");
        }
        ResearchCheck check = new ResearchCheck();
        check.setCheckNo(genNo("CK"));
        check.setUserId(riskLog.getUserId());
        check.setAchId(riskLog.getAchId());
        check.setRiskSource("AUTO");
        check.setRiskType(riskLog.getRiskType());
        check.setRiskDesc(riskLog.getRiskDesc());
        check.setRiskLogId(riskLogId);
        check.setStatus("PROCESSING");
        check.setAssignee(SecurityUtils.currentUserId());
        check.setClaimTime(LocalDateTime.now());
        checkMapper.insert(check);
        riskLog.setStatus("CLAIMED");
        riskLog.setCheckId(check.getCheckId());
        riskLogMapper.updateById(riskLog);
        return check;
    }

    // ---------------- 工单流转 ----------------

    @Transactional
    public void claim(Long checkId) {
        requireOperator();
        ResearchCheck check = getCheck(checkId);
        if (!"PENDING".equals(check.getStatus())) {
            throw new BusinessException("仅待认领工单可认领");
        }
        check.setStatus("PROCESSING");
        check.setAssignee(SecurityUtils.currentUserId());
        check.setClaimTime(LocalDateTime.now());
        checkMapper.updateById(check);
    }

    public void record(Long checkId, CheckRecordDTO dto) {
        requireOperator();
        getCheck(checkId);
        ResearchCheckRecord record = new ResearchCheckRecord();
        record.setCheckId(checkId);
        record.setRecordType(dto.getRecordType());
        record.setContent(dto.getContent());
        record.setOperator(SecurityUtils.currentUserId());
        record.setOperatorName(SecurityUtils.currentUser().getRealName());
        recordMapper.insert(record);
    }

    @Transactional
    public void confirm(Long checkId, CheckConfirmDTO dto) {
        requireOperator();
        ResearchCheck check = getCheck(checkId);
        if (!"PROCESSING".equals(check.getStatus())) {
            throw new BusinessException("仅核查中的工单可认定");
        }
        if (dto.getViolationType() == null || dto.getViolationLevel() == null) {
            throw new BusinessException("违规类型与等级不能为空");
        }
        ScoreEngine.DeductRule rule = scoreEngine.deductRule(dto.getViolationType());
        boolean veto = rule.veto();
        if (!veto && !"B".equals(dto.getViolationLevel()) && !"C".equals(dto.getViolationLevel())
                && !"D".equals(dto.getViolationLevel())) {
            throw new BusinessException("违规等级必须为 B/C/D");
        }
        BigDecimal deduct = dto.getDeductScore() != null ? dto.getDeductScore() : rule.deductScore();

        ResearchViolation violation = new ResearchViolation();
        violation.setUserId(check.getUserId());
        violation.setAchId(check.getAchId());
        violation.setCheckId(checkId);
        violation.setViolationType(dto.getViolationType());
        violation.setViolationLevel(dto.getViolationLevel());
        violation.setDeductScore(deduct);
        violation.setDescription(dto.getDescription());
        violation.setEvidence(dto.getEvidence());
        violation.setStatus("CONFIRMED");
        violation.setVetoFlag(veto ? 1 : 0);
        violation.setReformDeadline(dto.getReformDeadline() != null
                ? dto.getReformDeadline() : LocalDate.now().plusDays(15));
        violation.setCreateBy(SecurityUtils.currentUserId());
        violationMapper.insert(violation);

        check.setStatus("CONFIRMED");
        check.setConclusion(dto.getDescription());
        check.setDeductScore(deduct);
        check.setViolationId(violation.getViolationId());
        checkMapper.updateById(check);

        record(checkId, CheckRecordDTO.of("CONFIRM", "认定违规：" + dto.getViolationType()
                + "，" + dto.getViolationLevel() + "级，扣分 " + deduct));
        noticeService.send(check.getUserId(), "RISK", "失信认定通知",
                "您被认定存在" + dto.getViolationType() + "（" + dto.getViolationLevel() + "级），如有异议请在公示期内申诉",
                "CHECK", checkId);
    }

    @Transactional
    public void dismiss(Long checkId) {
        requireOperator();
        ResearchCheck check = getCheck(checkId);
        if (!"PENDING".equals(check.getStatus()) && !"PROCESSING".equals(check.getStatus())) {
            throw new BusinessException("当前状态不可撤销为误报");
        }
        check.setStatus("DISMISSED");
        check.setHandleResult("核查后确认误报，撤销");
        check.setFinishTime(LocalDateTime.now());
        checkMapper.updateById(check);
        if (check.getRiskLogId() != null) {
            ResearchRiskLog riskLog = riskLogMapper.selectById(check.getRiskLogId());
            if (riskLog != null) {
                riskLog.setStatus("DISMISSED");
                riskLogMapper.updateById(riskLog);
            }
        }
        record(checkId, CheckRecordDTO.of("HANDLE", "误报撤销"));
    }

    @Transactional
    public void publish(Long checkId) {
        requireOperator();
        ResearchCheck check = getCheck(checkId);
        if (!"CONFIRMED".equals(check.getStatus())) {
            throw new BusinessException("仅已认定的工单可发起公示");
        }
        ResearchPublicity publicity = publicityService.create(new com.hospital.integrity.dto.PublicityDTO(),
                "CONFIRM", "CHECK", checkId);
        check.setStatus("TO_PUBLIC");
        check.setPublicityId(publicity.getPublicityId());
        checkMapper.updateById(check);
        record(checkId, CheckRecordDTO.of("HANDLE", "发起失信认定公示"));
    }

    @Transactional
    public void effect(Long checkId) {
        requireOperator();
        ResearchCheck check = getCheck(checkId);
        if (!"TO_PUBLIC".equals(check.getStatus())) {
            throw new BusinessException("仅待公示生效状态的工单可生效");
        }
        ResearchViolation violation = check.getViolationId() == null ? null
                : violationMapper.selectById(check.getViolationId());
        if (violation == null) {
            throw new BusinessException("违规记录缺失，无法生效");
        }
        violation.setStatus("EFFECTIVE");
        violation.setEffectiveDate(LocalDate.now());
        violationMapper.updateById(violation);

        check.setStatus("PUBLISHED");
        check.setFinishTime(LocalDateTime.now());
        checkMapper.updateById(check);
        record(checkId, CheckRecordDTO.of("HANDLE", "公示结束，扣分生效"));

        // 重新计算该用户当年评价
        integrityService.recalcUserYear(check.getUserId(), LocalDate.now().getYear());
        noticeService.send(check.getUserId(), "EVALUATE", "扣分生效通知",
                "您的违规记录已生效（扣分 " + violation.getDeductScore() + " 分），年度评价已更新",
                "VIOLATION", violation.getViolationId());
    }

    @Transactional
    public void archive(Long checkId) {
        requireOperator();
        ResearchCheck check = getCheck(checkId);
        if (!"PUBLISHED".equals(check.getStatus())) {
            throw new BusinessException("仅已生效工单可归档");
        }
        check.setStatus("ARCHIVED");
        checkMapper.updateById(check);
        record(checkId, CheckRecordDTO.of("HANDLE", "工单归档"));
    }

    // ---------------- 违规整改 ----------------

    @Transactional
    public void reform(Long violationId, String result) {
        LoginUser current = SecurityUtils.currentUser();
        ResearchViolation violation = getViolation(violationId);
        if (!violation.getUserId().equals(current.getUserId())) {
            throw new BusinessException(403, "只能提交本人的整改");
        }
        if (!"EFFECTIVE".equals(violation.getStatus())) {
            throw new BusinessException("仅已生效的违规记录可提交整改");
        }
        violation.setStatus("REFORMING");
        violation.setReformResult(result);
        violationMapper.updateById(violation);
    }

    @Transactional
    public void reformCheck(Long violationId, boolean pass, String comment) {
        requireOperator();
        ResearchViolation violation = getViolation(violationId);
        if (!"REFORMING".equals(violation.getStatus())) {
            throw new BusinessException("仅整改中的记录可验收");
        }
        if (pass) {
            violation.setStatus("REFORMED");
            violation.setReformCheckBy(SecurityUtils.currentUserId());
            violation.setReformCheckTime(LocalDateTime.now());
        } else {
            violation.setStatus("EFFECTIVE");
            violation.setReformResult((violation.getReformResult() == null ? "" : violation.getReformResult())
                    + "（验收未通过：" + (comment == null ? "" : comment) + "）");
        }
        violationMapper.updateById(violation);
        // 验收通过后按规则减免，重算年度评价
        integrityService.recalcUserYear(violation.getUserId(),
                violation.getEffectiveDate() == null ? LocalDate.now().getYear() : violation.getEffectiveDate().getYear());
    }

    // ---------------- 查询 ----------------

    public PageResult<ResearchCheck> checkPage(int pageNum, int pageSize, String status) {
        requireOperator();
        Page<ResearchCheck> page = checkMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ResearchCheck>()
                        .eq(status != null && !status.isBlank(), ResearchCheck::getStatus, status)
                        .orderByDesc(ResearchCheck::getCreateTime));
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public PageResult<ResearchRiskLog> riskLogPage(int pageNum, int pageSize, String status, String riskType) {
        LambdaQueryWrapper<ResearchRiskLog> wrapper = new LambdaQueryWrapper<ResearchRiskLog>()
                .eq(status != null && !status.isBlank(), ResearchRiskLog::getStatus, status)
                .eq(riskType != null && !riskType.isBlank(), ResearchRiskLog::getRiskType, riskType)
                .orderByDesc(ResearchRiskLog::getCreateTime);
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasRole("auditor") && !SecurityUtils.hasRole("committee")) {
            wrapper.eq(ResearchRiskLog::getUserId, SecurityUtils.currentUserId());
        }
        Page<ResearchRiskLog> page = riskLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public PageResult<ResearchViolation> violationPage(int pageNum, int pageSize, String status, String level) {
        LambdaQueryWrapper<ResearchViolation> wrapper = new LambdaQueryWrapper<ResearchViolation>()
                .eq(status != null && !status.isBlank(), ResearchViolation::getStatus, status)
                .eq(level != null && !level.isBlank(), ResearchViolation::getViolationLevel, level)
                .orderByDesc(ResearchViolation::getCreateTime);
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasRole("auditor")
                && !SecurityUtils.hasRole("committee") && !SecurityUtils.hasRole("leader")) {
            wrapper.eq(ResearchViolation::getUserId, SecurityUtils.currentUserId());
        }
        Page<ResearchViolation> page = violationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public CheckVO detail(Long checkId) {
        ResearchCheck check = getCheck(checkId);
        CheckVO vo = new CheckVO();
        vo.setCheck(check);
        vo.setRecords(records(checkId));
        vo.setViolation(check.getViolationId() == null ? null : violationMapper.selectById(check.getViolationId()));
        return vo;
    }

    public List<ResearchCheckRecord> records(Long checkId) {
        return recordMapper.selectList(new LambdaQueryWrapper<ResearchCheckRecord>()
                .eq(ResearchCheckRecord::getCheckId, checkId)
                .orderByAsc(ResearchCheckRecord::getOperateTime));
    }

    public ResearchViolation violationOf(Long checkId) {
        ResearchCheck check = getCheck(checkId);
        return check.getViolationId() == null ? null : violationMapper.selectById(check.getViolationId());
    }

    // ---------------- 私有 ----------------

    private void requireOperator() {
        if (!SecurityUtils.hasRole("auditor") && !SecurityUtils.hasRole("committee") && !SecurityUtils.isAdmin()) {
            throw new BusinessException(403, "无权限操作核查工单");
        }
    }

    private ResearchCheck getCheck(Long checkId) {
        ResearchCheck check = checkMapper.selectById(checkId);
        if (check == null) {
            throw new BusinessException("工单不存在");
        }
        return check;
    }

    private ResearchViolation getViolation(Long violationId) {
        ResearchViolation violation = violationMapper.selectById(violationId);
        if (violation == null) {
            throw new BusinessException("违规记录不存在");
        }
        return violation;
    }

    private String genNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (int) (Math.random() * 90 + 10);
    }
}
