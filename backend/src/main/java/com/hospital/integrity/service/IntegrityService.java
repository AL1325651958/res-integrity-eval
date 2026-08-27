package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.entity.*;
import com.hospital.integrity.mapper.*;
import com.hospital.integrity.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 年度诚信评价：业绩分 + 有效扣分 → 总分 → A/B/C/D 等级，明细快照可追溯。
 * 规则版本与等级阈值读取 research_level_config / research_rule。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrityService {

    private final ResearchIntegrityMapper integrityMapper;
    private final ResearchIntegrityDetailMapper detailMapper;
    private final ResearchAchievementMapper achievementMapper;
    private final ResearchViolationMapper violationMapper;
    private final ResearchLevelConfigMapper levelConfigMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;

    /** 科室名缓存 */
    private final Map<Long, String> deptNameCache = new HashMap<>();

    /** 全量年度评价（定时任务 / 手动触发） */
    @Transactional
    public int calcYear(int year) {
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .eq(SysUser::getDelFlag, 0));
        int count = 0;
        for (SysUser user : users) {
            try {
                recalcUserYear(user.getUserId(), year);
                count++;
            } catch (Exception e) {
                log.error("年度评价计算失败: userId={}, year={}", user.getUserId(), year, e);
            }
        }
        return count;
    }

    /** 单用户单年度评价（重算：作废/扣分生效/整改验收/申诉变更时调用） */
    @Transactional
    public void recalcUserYear(Long userId, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        // 1. 业绩总分
        List<ResearchAchievement> achs = achievementMapper.selectList(new LambdaQueryWrapper<ResearchAchievement>()
                .eq(ResearchAchievement::getUserId, userId)
                .eq(ResearchAchievement::getStatus, 3)
                .eq(ResearchAchievement::getScoreStatus, 1)
                .between(ResearchAchievement::getPublishTime, yearStart.atStartOfDay(), yearEnd.plusDays(1).atStartOfDay()));
        BigDecimal perf = achs.stream()
                .map(ResearchAchievement::getScore)
                .filter(s -> s != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. 有效扣分（整改减免：B 减 50%、C 减 30%；D 不减免）
        List<ResearchViolation> violations = violationMapper.selectList(new LambdaQueryWrapper<ResearchViolation>()
                .eq(ResearchViolation::getUserId, userId)
                .in(ResearchViolation::getStatus, "EFFECTIVE", "REFORMING", "REFORMED")
                .between(ResearchViolation::getEffectiveDate, yearStart, yearEnd));
        BigDecimal deduct = BigDecimal.ZERO;
        boolean veto = false;
        long cLevelCount = 0;
        boolean unreformed = false;
        for (ResearchViolation v : violations) {
            if (v.getVetoFlag() != null && v.getVetoFlag() == 1) {
                veto = true;
                continue;
            }
            BigDecimal score = v.getDeductScore() == null ? BigDecimal.ZERO : v.getDeductScore();
            if ("REFORMED".equals(v.getStatus())) {
                score = score.multiply(reductionRate(v.getViolationLevel()));
            }
            deduct = deduct.add(score);
            if ("C".equals(v.getViolationLevel())) {
                cLevelCount++;
            }
            if (("EFFECTIVE".equals(v.getStatus()) || "REFORMING".equals(v.getStatus()))
                    && ("B".equals(v.getViolationLevel()) || "C".equals(v.getViolationLevel()))) {
                unreformed = true;
            }
        }

        // 3. 等级判定
        String level = judgeLevel(deduct, veto, cLevelCount, unreformed);

        // 4. 写汇总（唯一：user+year+period_type）
        ResearchIntegrity existing = integrityMapper.selectOne(new LambdaQueryWrapper<ResearchIntegrity>()
                .eq(ResearchIntegrity::getUserId, userId)
                .eq(ResearchIntegrity::getYear, year)
                .eq(ResearchIntegrity::getPeriodType, "YEAR"));
        ResearchIntegrity integrity = existing == null ? new ResearchIntegrity() : existing;
        integrity.setUserId(userId);
        integrity.setYear(year);
        integrity.setPeriodType("YEAR");
        integrity.setPerfScore(perf);
        integrity.setDeductScore(deduct);
        integrity.setTotalScore(perf.subtract(deduct));
        integrity.setLevel(level);
        integrity.setVetoFlag(veto ? 1 : 0);
        integrity.setRuleVersion(currentRuleVersion(year));
        integrity.setCalcStatus(existing == null ? 1 : 2);
        if (existing == null) {
            integrityMapper.insert(integrity);
        } else {
            integrityMapper.updateById(integrity);
        }

        // 5. 明细快照
        detailMapper.delete(new LambdaQueryWrapper<ResearchIntegrityDetail>()
                .eq(ResearchIntegrityDetail::getIntegrityId, integrity.getIntegrityId()));
        for (ResearchAchievement ach : achs) {
            ResearchIntegrityDetail d = new ResearchIntegrityDetail();
            d.setIntegrityId(integrity.getIntegrityId());
            d.setUserId(userId);
            d.setYear(year);
            d.setBizType("PERF");
            d.setAchId(ach.getAchId());
            d.setItemName(ach.getTitle());
            d.setBaseScore(ach.getScore());
            d.setScore(ach.getScore());
            d.setRuleVersion(integrity.getRuleVersion());
            detailMapper.insert(d);
        }
        for (ResearchViolation v : violations) {
            ResearchIntegrityDetail d = new ResearchIntegrityDetail();
            d.setIntegrityId(integrity.getIntegrityId());
            d.setUserId(userId);
            d.setYear(year);
            d.setBizType("DEDUCT");
            d.setViolationId(v.getViolationId());
            d.setItemName((v.getViolationType() == null ? "违规" : v.getViolationType()) + "（" + v.getViolationLevel() + "级）");
            d.setBaseScore(v.getDeductScore());
            d.setScore(v.getDeductScore() == null ? BigDecimal.ZERO : v.getDeductScore().negate());
            d.setRuleVersion(integrity.getRuleVersion());
            detailMapper.insert(d);
        }
        log.info("年度评价完成: userId={}, year={}, level={}, perf={}, deduct={}",
                userId, year, level, perf, deduct);
    }

    public ResearchIntegrity myIntegrity(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        return integrityMapper.selectOne(new LambdaQueryWrapper<ResearchIntegrity>()
                .eq(ResearchIntegrity::getUserId, SecurityUtils.currentUserId())
                .eq(ResearchIntegrity::getYear, y)
                .eq(ResearchIntegrity::getPeriodType, "YEAR"));
    }

    public List<ResearchIntegrityDetail> myDetail(Integer year) {
        ResearchIntegrity integrity = myIntegrity(year);
        if (integrity == null) {
            return List.of();
        }
        return detailMapper.selectList(new LambdaQueryWrapper<ResearchIntegrityDetail>()
                .eq(ResearchIntegrityDetail::getIntegrityId, integrity.getIntegrityId())
                .orderByAsc(ResearchIntegrityDetail::getBizType));
    }

    public ResearchIntegrity userIntegrity(Long userId, Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        return integrityMapper.selectOne(new LambdaQueryWrapper<ResearchIntegrity>()
                .eq(ResearchIntegrity::getUserId, userId)
                .eq(ResearchIntegrity::getYear, y)
                .eq(ResearchIntegrity::getPeriodType, "YEAR"));
    }

    /** 某用户某年度评价明细（管理端查看） */
    public Map<String, Object> userDetail(Long userId, Integer year) {
        ResearchIntegrity integrity = userIntegrity(userId, year);
        if (integrity == null) {
            return Map.of("integrity", null, "details", List.of());
        }
        List<ResearchIntegrityDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<ResearchIntegrityDetail>()
                        .eq(ResearchIntegrityDetail::getIntegrityId, integrity.getIntegrityId())
                        .orderByAsc(ResearchIntegrityDetail::getBizType));
        return Map.of("integrity", integrity, "details", details);
    }

    // ---------------- 私有 ----------------

    private BigDecimal reductionRate(String level) {
        if ("B".equals(level)) {
            return new BigDecimal("0.5");
        }
        if ("C".equals(level)) {
            return new BigDecimal("0.7");
        }
        return BigDecimal.ONE;
    }

    private String judgeLevel(BigDecimal deduct, boolean veto, long cLevelCount, boolean unreformed) {
        if (veto || deduct.compareTo(new BigDecimal("30")) > 0 || cLevelCount >= 2) {
            return "D";
        }
        if (unreformed) {
            return "C";
        }
        if (deduct.compareTo(BigDecimal.ZERO) == 0) {
            return "A";
        }
        if (deduct.compareTo(new BigDecimal("10")) <= 0) {
            return "B";
        }
        return "C";
    }

    private String currentRuleVersion(int year) {
        ResearchLevelConfig cfg = levelConfigMapper.selectOne(new LambdaQueryWrapper<ResearchLevelConfig>()
                .eq(ResearchLevelConfig::getStatus, 1)
                .le(ResearchLevelConfig::getEffectiveDate, LocalDate.of(year, 12, 31))
                .orderByDesc(ResearchLevelConfig::getEffectiveDate)
                .last("LIMIT 1"));
        return cfg == null ? "1.0" : cfg.getVersion();
    }

    // ---------------- 查询（管理端） ----------------

    public record IntegrityRow(ResearchIntegrity integrity, String realName, String deptName) {
    }

    public PageResult<IntegrityRow> list(int pageNum, int pageSize, Integer year, String level, Long deptId, String keyword) {
        int y = year == null ? LocalDate.now().getYear() : year;
        LambdaQueryWrapper<ResearchIntegrity> wrapper = new LambdaQueryWrapper<ResearchIntegrity>()
                .eq(ResearchIntegrity::getYear, y)
                .eq(level != null && !level.isBlank(), ResearchIntegrity::getLevel, level);
        if (deptId != null || (keyword != null && !keyword.isBlank())) {
            wrapper.inSql(ResearchIntegrity::getUserId, buildUserSubSql(deptId, keyword));
        }
        wrapper.orderByDesc(ResearchIntegrity::getTotalScore);
        Page<ResearchIntegrity> page = integrityMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, SysUser> userMap = page.getRecords().isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(page.getRecords().stream().map(ResearchIntegrity::getUserId).toList())
                .stream().collect(Collectors.toMap(SysUser::getUserId, u -> u));
        List<IntegrityRow> rows = page.getRecords().stream().map(i -> {
            SysUser u = userMap.get(i.getUserId());
            return new IntegrityRow(i, u == null ? null : u.getRealName(), u == null ? null : u.getDeptId() == null ? null : deptNameOf(u.getDeptId()));
        }).toList();
        return PageResult.of(page.getTotal(), rows);
    }

    private String deptNameOf(Long deptId) {
        return deptNameCache.computeIfAbsent(deptId, id -> {
            SysDept dept = sysDeptMapper.selectById(id);
            return dept == null ? null : dept.getDeptName();
        });
    }

    private String buildUserSubSql(Long deptId, String keyword) {
        StringBuilder sb = new StringBuilder("SELECT user_id FROM sys_user WHERE 1=1");
        if (deptId != null) {
            sb.append(" AND dept_id = ").append(deptId);
        }
        if (keyword != null && !keyword.isBlank()) {
            sb.append(" AND (real_name LIKE '%").append(keyword).append("%' OR emp_no LIKE '%").append(keyword).append("%')");
        }
        return sb.toString();
    }

    public void checkExists(Long integrityId) {
        if (integrityMapper.selectById(integrityId) == null) {
            throw new BusinessException("评价记录不存在");
        }
    }
}
