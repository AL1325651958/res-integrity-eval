package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospital.integrity.entity.*;
import com.hospital.integrity.mapper.*;
import com.hospital.integrity.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 三级看板统计：个人 / 科室 / 全院
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ResearchAchievementMapper achievementMapper;
    private final ResearchIntegrityMapper integrityMapper;
    private final ResearchViolationMapper violationMapper;
    private final ResearchRiskLogMapper riskLogMapper;
    private final ResearchCheckMapper checkMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /** 个人看板 */
    public Map<String, Object> my() {
        Long userId = SecurityUtils.currentUserId();
        Map<String, Object> data = new LinkedHashMap<>();

        ResearchIntegrity integrity = latestIntegrity(userId);
        if (integrity != null) {
            data.put("year", integrity.getYear());
            data.put("yearScore", integrity.getTotalScore());
            data.put("perfScore", integrity.getPerfScore());
            data.put("deductScore", integrity.getDeductScore());
            data.put("level", integrity.getLevel());
            data.put("vetoFlag", integrity.getVetoFlag());
        } else {
            data.put("year", LocalDate.now().getYear());
            data.put("yearScore", 0);
            data.put("perfScore", 0);
            data.put("deductScore", 0);
            data.put("level", null);
            data.put("vetoFlag", 0);
        }
        data.put("achStats", achievementMapper.selectMaps(new QueryWrapper<ResearchAchievement>()
                .select("ach_type as achType, count(*) as cnt")
                .eq("user_id", userId).eq("status", 3)
                .groupBy("ach_type")));
        data.put("deductList", violationMapper.selectList(new LambdaQueryWrapper<ResearchViolation>()
                .eq(ResearchViolation::getUserId, userId)
                .ne(ResearchViolation::getStatus, "REVOKED")
                .orderByDesc(ResearchViolation::getCreateTime)
                .last("LIMIT 10")));
        data.put("pendingCount", achievementMapper.selectCount(new LambdaQueryWrapper<ResearchAchievement>()
                .eq(ResearchAchievement::getUserId, userId)
                .in(ResearchAchievement::getStatus, 1, 2)));
        data.put("riskCount", riskLogMapper.selectCount(new LambdaQueryWrapper<ResearchRiskLog>()
                .eq(ResearchRiskLog::getUserId, userId)
                .eq(ResearchRiskLog::getStatus, "NEW")));
        return data;
    }

    /** 科室看板 */
    public Map<String, Object> dept() {
        Long deptId = SecurityUtils.currentUser().getDeptId();
        Map<String, Object> data = new LinkedHashMap<>();
        List<Long> userIds = deptUserIds(deptId);
        if (userIds.isEmpty()) {
            return emptyDept(deptId);
        }
        data.put("deptId", deptId);
        data.put("achTotal", achievementMapper.selectCount(new LambdaQueryWrapper<ResearchAchievement>()
                .eq(ResearchAchievement::getStatus, 3).in(ResearchAchievement::getUserId, userIds)));
        data.put("typeDist", achievementMapper.selectMaps(new QueryWrapper<ResearchAchievement>()
                .select("ach_type as achType, count(*) as cnt")
                .eq("status", 3).in("user_id", userIds)
                .groupBy("ach_type")));
        int year = LocalDate.now().getYear();
        data.put("levelDist", integrityMapper.selectMaps(new QueryWrapper<ResearchIntegrity>()
                .select("level, count(*) as cnt")
                .eq("year", year).in("user_id", userIds)
                .groupBy("level")));
        data.put("riskUsers", riskLogMapper.selectMaps(new QueryWrapper<ResearchRiskLog>()
                .select("user_id as userId, count(*) as cnt")
                .eq("status", "NEW").in("user_id", userIds)
                .groupBy("user_id")));
        data.put("unAudited", achievementMapper.selectCount(new LambdaQueryWrapper<ResearchAchievement>()
                .eq(ResearchAchievement::getStatus, 1).in(ResearchAchievement::getUserId, userIds)));
        return data;
    }

    /** 全院看板 */
    public Map<String, Object> hospital() {
        Map<String, Object> data = new LinkedHashMap<>();
        int year = LocalDate.now().getYear();
        List<ResearchIntegrity> yearIntegrities = integrityMapper.selectList(
                new LambdaQueryWrapper<ResearchIntegrity>().eq(ResearchIntegrity::getYear, year));
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1).eq(SysUser::getDelFlag, 0));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("users", users.size());
        summary.put("achTotal", achievementMapper.selectCount(new LambdaQueryWrapper<ResearchAchievement>()
                .eq(ResearchAchievement::getStatus, 3)));
        double avg = yearIntegrities.stream()
                .mapToDouble(i -> i.getTotalScore() == null ? 0 : i.getTotalScore().doubleValue())
                .average().orElse(0);
        summary.put("avgScore", Math.round(avg * 100) / 100.0);
        data.put("summary", summary);

        // 科室排名（按年度平均总分）
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getUserId, u -> u));
        Map<Long, List<Double>> deptScores = new HashMap<>();
        for (ResearchIntegrity i : yearIntegrities) {
            SysUser u = userMap.get(i.getUserId());
            if (u == null || u.getDeptId() == null) {
                continue;
            }
            deptScores.computeIfAbsent(u.getDeptId(), k -> new ArrayList<>())
                    .add(i.getTotalScore() == null ? 0 : i.getTotalScore().doubleValue());
        }
        List<Map<String, Object>> deptRank = deptScores.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            SysDept dept = deptMapper.selectById(e.getKey());
            m.put("deptName", dept == null ? String.valueOf(e.getKey()) : dept.getDeptName());
            m.put("avgScore", Math.round(e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) / 100.0);
            m.put("userCount", e.getValue().size());
            return m;
        }).sorted((a, b) -> Double.compare((Double) b.get("avgScore"), (Double) a.get("avgScore")))
                .limit(10).toList();
        data.put("deptRank", deptRank);

        data.put("riskTypeStat", riskLogMapper.selectMaps(new QueryWrapper<ResearchRiskLog>()
                .select("risk_type as riskType, count(*) as cnt")
                .eq("status", "NEW")
                .groupBy("risk_type")));

        // 年度成果趋势（近5年）
        int currentYear = LocalDate.now().getYear();
        List<Map<String, Object>> yearTrend = new ArrayList<>();
        for (int y = currentYear - 4; y <= currentYear; y++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("year", y);
            m.put("cnt", achievementMapper.selectCount(new LambdaQueryWrapper<ResearchAchievement>()
                    .eq(ResearchAchievement::getStatus, 3)
                    .apply("YEAR(publish_time) = {0}", y)));
            yearTrend.add(m);
        }
        data.put("yearTrend", yearTrend);

        // 严重失信名单（D 级）
        List<Map<String, Object>> seriousList = yearIntegrities.stream()
                .filter(i -> "D".equals(i.getLevel()))
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    SysUser u = userMap.get(i.getUserId());
                    m.put("realName", u == null ? null : u.getRealName());
                    m.put("totalScore", i.getTotalScore());
                    m.put("year", i.getYear());
                    return m;
                }).toList();
        data.put("seriousList", seriousList);

        data.put("pendingChecks", checkMapper.selectCount(new LambdaQueryWrapper<ResearchCheck>()
                .in(ResearchCheck::getStatus, "PENDING", "PROCESSING", "TO_CONFIRM", "TO_PUBLIC")));
        return data;
    }

    // ---------------- 私有 ----------------

    private ResearchIntegrity latestIntegrity(Long userId) {
        List<ResearchIntegrity> list = integrityMapper.selectList(new LambdaQueryWrapper<ResearchIntegrity>()
                .eq(ResearchIntegrity::getUserId, userId)
                .eq(ResearchIntegrity::getPeriodType, "YEAR")
                .orderByDesc(ResearchIntegrity::getYear)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private List<Long> deptUserIds(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDeptId, deptId)
                        .eq(SysUser::getDelFlag, 0))
                .stream().map(SysUser::getUserId).toList();
    }

    private Map<String, Object> emptyDept(Long deptId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deptId", deptId);
        data.put("achTotal", 0);
        data.put("typeDist", List.of());
        data.put("levelDist", List.of());
        data.put("riskUsers", List.of());
        data.put("unAudited", 0);
        return data;
    }
}
