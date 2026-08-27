package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.dto.AppealDTO;
import com.hospital.integrity.entity.*;
import com.hospital.integrity.mapper.*;
import com.hospital.integrity.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 申诉：提交 → 委员会受理复核 → 维持/变更/驳回，全程留痕
 */
@Service
@RequiredArgsConstructor
public class AppealService {

    private final ResearchAppealMapper appealMapper;
    private final ResearchViolationMapper violationMapper;
    private final ResearchCheckMapper checkMapper;
    private final ResearchIntegrityMapper integrityMapper;
    private final IntegrityService integrityService;
    private final NoticeService noticeService;

    @Transactional
    public ResearchAppeal create(AppealDTO dto) {
        Long userId = SecurityUtils.currentUserId();
        // 校验目标归属
        switch (dto.getBizType()) {
            case "VIOLATION" -> {
                ResearchViolation v = violationMapper.selectById(dto.getBizId());
                if (v == null || !v.getUserId().equals(userId)) {
                    throw new BusinessException("申诉目标不存在或不属于本人");
                }
            }
            case "CHECK" -> {
                ResearchCheck c = checkMapper.selectById(dto.getBizId());
                if (c == null || !c.getUserId().equals(userId)) {
                    throw new BusinessException("申诉目标不存在或不属于本人");
                }
            }
            case "INTEGRITY" -> {
                ResearchIntegrity i = integrityMapper.selectById(dto.getBizId());
                if (i == null || !i.getUserId().equals(userId)) {
                    throw new BusinessException("申诉目标不存在或不属于本人");
                }
            }
            default -> throw new BusinessException("不支持的申诉对象类型");
        }
        ResearchAppeal appeal = new ResearchAppeal();
        appeal.setAppealNo(genNo());
        appeal.setUserId(userId);
        appeal.setAppealType(dto.getAppealType());
        appeal.setBizType(dto.getBizType());
        appeal.setBizId(dto.getBizId());
        appeal.setReason(dto.getReason());
        appeal.setStatus("PENDING");
        appealMapper.insert(appeal);
        return appeal;
    }

    public PageResult<ResearchAppeal> page(int pageNum, int pageSize, String status) {
        LambdaQueryWrapper<ResearchAppeal> wrapper = new LambdaQueryWrapper<ResearchAppeal>()
                .eq(status != null && !status.isBlank(), ResearchAppeal::getStatus, status)
                .orderByDesc(ResearchAppeal::getCreateTime);
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasRole("committee")) {
            wrapper.eq(ResearchAppeal::getUserId, SecurityUtils.currentUserId());
        }
        Page<ResearchAppeal> page = appealMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public ResearchAppeal detail(Long id) {
        ResearchAppeal appeal = appealMapper.selectById(id);
        if (appeal == null) {
            throw new BusinessException("申诉记录不存在");
        }
        return appeal;
    }

    @Transactional
    public void review(Long id, String pass, String result) {
        if (!SecurityUtils.isAdmin() && !SecurityUtils.hasRole("committee")) {
            throw new BusinessException(403, "无权限复核申诉");
        }
        ResearchAppeal appeal = appealMapper.selectById(id);
        if (appeal == null) {
            throw new BusinessException("申诉记录不存在");
        }
        if (!"PENDING".equals(appeal.getStatus()) && !"REVIEWING".equals(appeal.getStatus())) {
            throw new BusinessException("该申诉已裁定");
        }
        appeal.setStatus(pass);
        appeal.setResult(result);
        appeal.setResultBy(SecurityUtils.currentUserId());
        appeal.setResultTime(LocalDateTime.now());
        appealMapper.updateById(appeal);

        // 变更裁定：撤销违规并重算评价
        if ("OVERTURNED".equals(pass)) {
            if ("VIOLATION".equals(appeal.getBizType())) {
                ResearchViolation violation = violationMapper.selectById(appeal.getBizId());
                if (violation != null && !"REVOKED".equals(violation.getStatus())) {
                    violation.setStatus("REVOKED");
                    violationMapper.updateById(violation);
                    integrityService.recalcUserYear(violation.getUserId(),
                            violation.getEffectiveDate() == null
                                    ? LocalDateTime.now().getYear() : violation.getEffectiveDate().getYear());
                }
            } else if ("INTEGRITY".equals(appeal.getBizType())) {
                ResearchIntegrity integrity = integrityMapper.selectById(appeal.getBizId());
                if (integrity != null) {
                    integrityService.recalcUserYear(integrity.getUserId(), integrity.getYear());
                }
            }
        }
        noticeService.send(appeal.getUserId(), "APPEAL", "申诉复核结果通知",
                "您的申诉（" + appeal.getAppealNo() + "）复核结果：" + label(pass)
                        + (result == null ? "" : "，" + result), "APPEAL", appeal.getAppealId());
    }

    private String label(String pass) {
        return switch (pass) {
            case "SUSTAINED" -> "维持原判";
            case "OVERTURNED" -> "变更裁定";
            case "REJECTED" -> "驳回";
            default -> pass;
        };
    }

    private String genNo() {
        return "AP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (int) (Math.random() * 90 + 10);
    }
}
