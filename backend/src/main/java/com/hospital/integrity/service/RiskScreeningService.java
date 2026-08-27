package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.integrity.entity.ResearchAchievement;
import com.hospital.integrity.entity.ResearchBlacklist;
import com.hospital.integrity.entity.ResearchRiskLog;
import com.hospital.integrity.mapper.ResearchAchievementMapper;
import com.hospital.integrity.mapper.ResearchBlacklistMapper;
import com.hospital.integrity.mapper.ResearchRiskLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 智能风控筛查：成果入库实时触发 + 每日定时任务。
 * 命中即写 research_risk_log 并通知本人。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScreeningService {

    private final ResearchAchievementMapper achievementMapper;
    private final ResearchBlacklistMapper blacklistMapper;
    private final ResearchRiskLogMapper riskLogMapper;
    private final NoticeService noticeService;

    /**
     * 对单条成果执行风控筛查（入库时调用）
     */
    public void screen(ResearchAchievement ach) {
        // 1. 成果重复申报：同人同编号重复填报
        if (ach.getAchNo() != null && !ach.getAchNo().isBlank()) {
            Long dup = achievementMapper.selectCount(new LambdaQueryWrapper<ResearchAchievement>()
                    .eq(ResearchAchievement::getUserId, ach.getUserId())
                    .eq(ResearchAchievement::getAchNo, ach.getAchNo())
                    .in(ResearchAchievement::getStatus, 1, 2, 3)
                    .ne(ResearchAchievement::getAchId, ach.getAchId()));
            if (dup != null && dup > 0) {
                hit(ach, "DUP_APPLY", "同一编号成果重复申报", ach.getAchNo());
            }
        }

        // 2. 一稿多投：DOI 已被他人入库论文使用 / 同人同标题多篇论文
        if ("PAPER".equals(ach.getAchType())) {
            if (ach.getAchNo() != null && !ach.getAchNo().isBlank()) {
                Long others = achievementMapper.selectCount(new LambdaQueryWrapper<ResearchAchievement>()
                        .eq(ResearchAchievement::getAchType, "PAPER")
                        .eq(ResearchAchievement::getAchNo, ach.getAchNo())
                        .eq(ResearchAchievement::getStatus, 3)
                        .ne(ResearchAchievement::getUserId, ach.getUserId())
                        .ne(ResearchAchievement::getAchId, ach.getAchId()));
                if (others != null && others > 0) {
                    hit(ach, "MULTI_SUBMIT", "同一 DOI 已被他人论文使用，疑似一稿多投", ach.getAchNo());
                }
            }
            if (ach.getTitle() != null) {
                Long sameTitle = achievementMapper.selectCount(new LambdaQueryWrapper<ResearchAchievement>()
                        .eq(ResearchAchievement::getAchType, "PAPER")
                        .eq(ResearchAchievement::getTitle, ach.getTitle())
                        .eq(ResearchAchievement::getStatus, 3)
                        .ne(ResearchAchievement::getAchId, ach.getAchId()));
                if (sameTitle != null && sameTitle > 0) {
                    hit(ach, "MULTI_SUBMIT", "存在标题完全相同的已入库论文，疑似重复发表", ach.getTitle());
                }
            }
        }

        // 3. 黑名单期刊 / 违规关键词
        List<ResearchBlacklist> journals = blacklistMapper.selectList(
                new LambdaQueryWrapper<ResearchBlacklist>()
                        .eq(ResearchBlacklist::getBlType, "JOURNAL")
                        .eq(ResearchBlacklist::getStatus, 1));
        String source = ach.getSourceName() == null ? "" : ach.getSourceName();
        journals.stream().filter(j -> source.contains(j.getBlName()))
                .findFirst().ifPresent(j -> hit(ach, "BLACKLIST_JOURNAL",
                        "期刊命中预警名单：" + j.getBlName() + "（来源 " + j.getSource() + "）", j.getBlName()));

        List<ResearchBlacklist> keywords = blacklistMapper.selectList(
                new LambdaQueryWrapper<ResearchBlacklist>()
                        .eq(ResearchBlacklist::getBlType, "KEYWORD")
                        .eq(ResearchBlacklist::getStatus, 1));
        String title = ach.getTitle() == null ? "" : ach.getTitle();
        keywords.stream().filter(k -> title.contains(k.getBlName()))
                .findFirst().ifPresent(k -> hit(ach, "OTHER", "标题命中违规关键词：" + k.getBlName(), k.getBlName()));

        // 4. 时间逻辑：发表时间晚于当前时间
        if (ach.getPublishTime() != null && ach.getPublishTime().isAfter(LocalDateTime.now().plusDays(1))) {
            hit(ach, "TIME_LOGIC", "发表/立项时间晚于当前时间，时间逻辑异常",
                    ach.getPublishTime().toString());
        }

        // 5. 署名异常：论文缺少作者位次
        if ("PAPER".equals(ach.getAchType()) && (ach.getRankInfo() == null || ach.getRankInfo().isBlank())) {
            hit(ach, "AUTHOR_ANOMALY", "论文缺少作者位次信息，署名信息不完整", ach.getTitle());
        }
    }

    /**
     * 定时任务：筛查最近 24 小时入库的成果
     */
    public void screenRecent() {
        List<ResearchAchievement> list = achievementMapper.selectList(
                new LambdaQueryWrapper<ResearchAchievement>()
                        .eq(ResearchAchievement::getStatus, 3)
                        .ge(ResearchAchievement::getUpdateTime, LocalDateTime.now().minus(1, ChronoUnit.DAYS)));
        for (ResearchAchievement ach : list) {
            try {
                screen(ach);
            } catch (Exception e) {
                log.error("成果[{}]风控筛查失败", ach.getAchId(), e);
            }
        }
    }

    private void hit(ResearchAchievement ach, String riskType, String desc, String matchValue) {
        // 相同成果同类型风险不重复记录
        Long exists = riskLogMapper.selectCount(new LambdaQueryWrapper<ResearchRiskLog>()
                .eq(ResearchRiskLog::getAchId, ach.getAchId())
                .eq(ResearchRiskLog::getRiskType, riskType)
                .eq(ResearchRiskLog::getStatus, "NEW"));
        if (exists != null && exists > 0) {
            return;
        }
        ResearchRiskLog log = new ResearchRiskLog();
        log.setUserId(ach.getUserId());
        log.setAchId(ach.getAchId());
        log.setRiskType(riskType);
        log.setRiskDesc(desc);
        log.setMatchValue(matchValue);
        log.setStatus("NEW");
        riskLogMapper.insert(log);
        noticeService.send(ach.getUserId(), "RISK", "科研诚信风险提醒",
                "您的成果《" + ach.getTitle() + "》触发风险筛查：" + desc, "RISK", log.getRiskId());
    }
}
