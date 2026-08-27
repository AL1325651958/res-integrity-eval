package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.dto.AchievementDTO;
import com.hospital.integrity.dto.AchievementVO;
import com.hospital.integrity.entity.*;
import com.hospital.integrity.mapper.*;
import com.hospital.integrity.security.LoginUser;
import com.hospital.integrity.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 成果填报、审核、计分、作废
 */
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final ResearchAchievementMapper achievementMapper;
    private final ResearchAttachmentMapper attachmentMapper;
    private final ResearchAuditLogMapper auditLogMapper;
    private final SysUserMapper sysUserMapper;
    private final ScoreEngine scoreEngine;
    private final RiskScreeningService riskScreeningService;
    private final IntegrityService integrityService;
    private final NoticeService noticeService;

    // ---------------- 填报 ----------------

    @Transactional
    public ResearchAchievement create(AchievementDTO dto) {
        LoginUser current = SecurityUtils.currentUser();
        ResearchAchievement ach = new ResearchAchievement();
        copyFields(ach, dto);
        ach.setUserId(current.getUserId());
        ach.setStatus(0);
        ach.setScoreStatus(0);
        ach.setCreateBy(current.getUserId());
        achievementMapper.insert(ach);
        bindAttachments(ach.getAchId(), dto.getAttachIds());
        return ach;
    }

    @Transactional
    public void update(Long id, AchievementDTO dto) {
        ResearchAchievement ach = getOwned(id);
        if (ach.getStatus() != 0 && ach.getStatus() != 4) {
            throw new BusinessException("仅草稿或已退回的成果可编辑");
        }
        copyFields(ach, dto);
        achievementMapper.updateById(ach);
        bindAttachments(id, dto.getAttachIds());
    }

    @Transactional
    public void delete(Long id) {
        ResearchAchievement ach = getOwned(id);
        if (ach.getStatus() == 0) {
            achievementMapper.deleteById(id);
        } else if (ach.getStatus() == 1 || ach.getStatus() == 2) {
            ach.setStatus(5);
            achievementMapper.updateById(ach);
            insertAuditLog(id, "CANCEL", "本人撤销申报");
        } else {
            throw new BusinessException("当前状态不可删除/撤销");
        }
    }

    @Transactional
    public void submit(Long id) {
        ResearchAchievement ach = getOwned(id);
        if (ach.getStatus() != 0 && ach.getStatus() != 4) {
            throw new BusinessException("仅草稿或已退回的成果可提交");
        }
        ach.setStatus(1);
        achievementMapper.updateById(ach);
        insertAuditLog(id, "SUBMIT", "提交审核");
    }

    // ---------------- 审核 ----------------

    @Transactional
    public void audit(Long id, AuditDTO dto) {
        ResearchAchievement ach = achievementMapper.selectById(id);
        if (ach == null) {
            throw new BusinessException("成果不存在");
        }
        LoginUser current = SecurityUtils.currentUser();
        if ("BACK".equals(dto.getAuditType()) && (dto.getOpinion() == null || dto.getOpinion().isBlank())) {
            throw new BusinessException("退回必须填写修改意见");
        }
        if (SecurityUtils.hasRole("dept_admin") && ach.getStatus() == 1) {
            // 科室初审
            if ("APPROVE".equals(dto.getAuditType())) {
                ach.setStatus(2);
            } else if ("BACK".equals(dto.getAuditType())) {
                ach.setStatus(4);
            } else {
                throw new BusinessException("非法审核类型");
            }
            ach.setAuditBy(current.getUserId());
            ach.setAuditRemark(dto.getOpinion());
            achievementMapper.updateById(ach);
            insertAuditLog(id, dto.getAuditType(), dto.getOpinion());
            notifyOwner(ach, "初审" + ("APPROVE".equals(dto.getAuditType()) ? "通过" : "退回"));
        } else if (SecurityUtils.hasRole("auditor") && ach.getStatus() == 2) {
            // 科研科终审
            if ("APPROVE".equals(dto.getAuditType())) {
                ach.setStatus(3);
                ach.setScoreStatus(1);
                // 自动计分
                BigDecimal score = scoreEngine.calcScore(ach);
                ach.setScore(score);
                ach.setAuditBy(current.getUserId());
                ach.setAuditTime(LocalDateTime.now());
                achievementMapper.updateById(ach);
                // 自动风控筛查
                riskScreeningService.screen(ach);
            } else if ("BACK".equals(dto.getAuditType())) {
                ach.setStatus(4);
                ach.setAuditBy(current.getUserId());
                achievementMapper.updateById(ach);
            } else {
                throw new BusinessException("非法审核类型");
            }
            insertAuditLog(id, dto.getAuditType(), dto.getOpinion());
            notifyOwner(ach, "终审" + ("APPROVE".equals(dto.getAuditType()) ? "通过并入库" : "退回"));
        } else {
            throw new BusinessException("当前状态不可执行该操作或无权操作");
        }
    }

    @Transactional
    public void invalidate(Long id) {
        if (!SecurityUtils.hasRole("auditor") && !SecurityUtils.isAdmin()) {
            throw new BusinessException(403, "无权限作废成果");
        }
        ResearchAchievement ach = achievementMapper.selectById(id);
        if (ach == null || ach.getStatus() != 3) {
            throw new BusinessException("仅已入库成果可作废");
        }
        ach.setStatus(6);
        ach.setScoreStatus(2);
        ach.setScore(BigDecimal.ZERO);
        achievementMapper.updateById(ach);
        insertAuditLog(id, "INVALID", "事后发现不端，作废并回收计分");
        if (ach.getPublishTime() != null) {
            integrityService.recalcUserYear(ach.getUserId(), ach.getPublishTime().getYear());
        }
    }

    // ---------------- 查询 ----------------

    public PageResult<ResearchAchievement> page(int pageNum, int pageSize, Integer status, String achType,
                                                String keyword, Integer year) {
        LoginUser current = SecurityUtils.currentUser();
        LambdaQueryWrapper<ResearchAchievement> wrapper = new LambdaQueryWrapper<>();
        applyDataScope(wrapper, current);
        wrapper.eq(status != null, ResearchAchievement::getStatus, status)
                .eq(achType != null && !achType.isBlank(), ResearchAchievement::getAchType, achType)
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(ResearchAchievement::getTitle, keyword)
                                .or().like(ResearchAchievement::getAchNo, keyword))
                .orderByDesc(ResearchAchievement::getCreateTime);
        if (year != null) {
            wrapper.apply("YEAR(publish_time) = {0}", year);
        }
        Page<ResearchAchievement> page = achievementMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public PageResult<ResearchAchievement> auditPage(int pageNum, int pageSize, String scope) {
        LoginUser current = SecurityUtils.currentUser();
        LambdaQueryWrapper<ResearchAchievement> wrapper = new LambdaQueryWrapper<>();
        if ("DEPT".equals(scope) && SecurityUtils.hasRole("dept_admin")) {
            wrapper.eq(ResearchAchievement::getStatus, 1)
                    .inSql(ResearchAchievement::getUserId,
                            "SELECT user_id FROM sys_user WHERE dept_id = " + current.getDeptId());
        } else if ("ALL".equals(scope) && SecurityUtils.hasRole("auditor")) {
            wrapper.eq(ResearchAchievement::getStatus, 2);
        } else {
            throw new BusinessException(403, "无权查看该审核列表");
        }
        wrapper.orderByAsc(ResearchAchievement::getCreateTime);
        Page<ResearchAchievement> page = achievementMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public AchievementVO detail(Long id) {
        ResearchAchievement ach = achievementMapper.selectById(id);
        if (ach == null) {
            throw new BusinessException("成果不存在");
        }
        AchievementVO vo = new AchievementVO();
        vo.setAchievement(ach);
        vo.setAttachments(attachmentMapper.selectList(new LambdaQueryWrapper<ResearchAttachment>()
                .eq(ResearchAttachment::getBizType, "ACH")
                .eq(ResearchAttachment::getBizId, id)));
        vo.setAuditLogs(auditLogMapper.selectList(new LambdaQueryWrapper<ResearchAuditLog>()
                .eq(ResearchAuditLog::getAchId, id)
                .orderByDesc(ResearchAuditLog::getAuditTime)));
        return vo;
    }

    /** 导出列表（数据范围同分页查询） */
    public List<ResearchAchievement> exportList(Integer status, String achType) {
        LoginUser current = SecurityUtils.currentUser();
        LambdaQueryWrapper<ResearchAchievement> wrapper = new LambdaQueryWrapper<>();
        applyDataScope(wrapper, current);
        wrapper.eq(status != null, ResearchAchievement::getStatus, status)
                .eq(achType != null && !achType.isBlank(), ResearchAchievement::getAchType, achType)
                .orderByDesc(ResearchAchievement::getCreateTime);
        return achievementMapper.selectList(wrapper);
    }

    // ---------------- 私有 ----------------

    private void applyDataScope(LambdaQueryWrapper<ResearchAchievement> wrapper, LoginUser current) {
        if (SecurityUtils.isAdmin() || SecurityUtils.hasRole("auditor")
                || SecurityUtils.hasRole("committee") || SecurityUtils.hasRole("leader")) {
            return; // 全部
        }
        if (SecurityUtils.hasRole("dept_admin")) {
            wrapper.inSql(ResearchAchievement::getUserId,
                    "SELECT user_id FROM sys_user WHERE dept_id = " + current.getDeptId());
            return;
        }
        wrapper.eq(ResearchAchievement::getUserId, current.getUserId());
    }

    private ResearchAchievement getOwned(Long id) {
        ResearchAchievement ach = achievementMapper.selectById(id);
        if (ach == null) {
            throw new BusinessException("成果不存在");
        }
        if (!ach.getUserId().equals(SecurityUtils.currentUserId())) {
            throw new BusinessException(403, "只能操作本人成果");
        }
        return ach;
    }

    private void copyFields(ResearchAchievement ach, AchievementDTO dto) {
        ach.setAchType(dto.getAchType());
        ach.setTitle(dto.getTitle());
        ach.setAchNo(dto.getAchNo());
        ach.setSourceName(dto.getSourceName());
        ach.setPublishTime(dto.getPublishTime());
        ach.setLevel(dto.getLevel());
        ach.setRankInfo(dto.getRankInfo());
        ach.setIsCorresponding(dto.getIsCorresponding());
        ach.setFundAmount(dto.getFundAmount());
    }

    private void bindAttachments(Long achId, List<Long> attachIds) {
        if (attachIds == null || attachIds.isEmpty()) {
            return;
        }
        attachmentMapper.update(null, new LambdaUpdateWrapper<ResearchAttachment>()
                .eq(ResearchAttachment::getBizType, "ACH")
                .in(ResearchAttachment::getFileId, attachIds)
                .set(ResearchAttachment::getBizId, achId));
    }

    private void insertAuditLog(Long achId, String auditType, String opinion) {
        LoginUser current = SecurityUtils.currentUser();
        ResearchAuditLog log = new ResearchAuditLog();
        log.setAchId(achId);
        log.setAuditType(auditType);
        log.setAuditBy(current.getUserId());
        log.setAuditName(current.getRealName());
        log.setOpinion(opinion);
        auditLogMapper.insert(log);
    }

    private void notifyOwner(ResearchAchievement ach, String action) {
        SysUser owner = sysUserMapper.selectById(ach.getUserId());
        if (owner != null) {
            noticeService.send(ach.getUserId(), "AUDIT", "成果审核通知",
                    "您的成果《" + ach.getTitle() + "》" + action, "ACH", ach.getAchId());
        }
    }
}
