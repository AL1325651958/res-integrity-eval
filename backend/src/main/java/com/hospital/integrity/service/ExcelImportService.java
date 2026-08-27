package com.hospital.integrity.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.entity.ResearchAchievement;
import com.hospital.integrity.entity.SysUser;
import com.hospital.integrity.mapper.ResearchAchievementMapper;
import com.hospital.integrity.mapper.SysUserMapper;
import com.hospital.integrity.security.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史科研成果 Excel 批量导入（生成草稿，走正常审核流程）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final ResearchAchievementMapper achievementMapper;
    private final SysUserMapper userMapper;

    /** 导入模板行 */
    @Data
    public static class ImportRow {
        @ExcelProperty("工号")
        private String empNo;
        @ExcelProperty("成果类型")
        private String achType;
        @ExcelProperty("标题")
        private String title;
        @ExcelProperty("编号")
        private String achNo;
        @ExcelProperty("来源")
        private String sourceName;
        @ExcelProperty("级别")
        private String level;
        @ExcelProperty("位次")
        private String rankInfo;
        @ExcelProperty("通讯作者(1是)")
        private String isCorresponding;
        @ExcelProperty("经费/到账金额")
        private BigDecimal fundAmount;
        @ExcelProperty("时间(yyyy-MM-dd)")
        private String publishTime;
    }

    public Map<String, Object> importAchievements(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        List<ImportRow> rows;
        try {
            rows = EasyExcel.read(file.getInputStream()).head(ImportRow.class).sheet().doReadSync();
        } catch (IOException e) {
            throw new BusinessException("读取Excel失败：" + e.getMessage());
        }
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("Excel无有效数据行");
        }
        List<String> errors = new ArrayList<>();
        int success = 0;
        Long defaultUserId = SecurityUtils.currentUserId();
        for (int i = 0; i < rows.size(); i++) {
            ImportRow row = rows.get(i);
            int line = i + 2; // 表头占1行
            try {
                ResearchAchievement ach = new ResearchAchievement();
                ach.setUserId(resolveUserId(row.getEmpNo(), defaultUserId));
                ach.setAchType(trim(row.getAchType()));
                ach.setTitle(trim(row.getTitle()));
                ach.setAchNo(trim(row.getAchNo()));
                ach.setSourceName(trim(row.getSourceName()));
                ach.setLevel(trim(row.getLevel()));
                ach.setRankInfo(trim(row.getRankInfo()));
                ach.setIsCorresponding("1".equals(trim(row.getIsCorresponding())) ? 1 : 0);
                ach.setFundAmount(row.getFundAmount());
                ach.setPublishTime(parseTime(row.getPublishTime()));
                if (ach.getAchType() == null || ach.getTitle() == null) {
                    throw new BusinessException("成果类型/标题不能为空");
                }
                ach.setStatus(0);
                ach.setScoreStatus(0);
                ach.setCreateBy(defaultUserId);
                achievementMapper.insert(ach);
                success++;
            } catch (Exception e) {
                errors.add("第" + line + "行：" + e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("success", success);
        result.put("errors", errors);
        return result;
    }

    public void downloadTemplate(HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=achievement_import_template.xlsx");
        try {
            EasyExcel.write(response.getOutputStream(), ImportRow.class).sheet("成果导入模板").doWrite(List.of());
        } catch (IOException e) {
            throw new BusinessException("模板下载失败");
        }
    }

    private Long resolveUserId(String empNo, Long defaultUserId) {
        if (empNo == null || empNo.isBlank()) {
            return defaultUserId;
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmpNo, empNo.trim())
                .eq(SysUser::getDelFlag, 0)
                .last("LIMIT 1"));
        return user == null ? defaultUserId : user.getUserId();
    }

    private LocalDateTime parseTime(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(s.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                throw new BusinessException("时间格式应为 yyyy-MM-dd");
            }
        }
    }

    private String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
