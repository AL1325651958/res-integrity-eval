package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.dto.AchievementDTO;
import com.hospital.integrity.dto.AchievementVO;
import com.hospital.integrity.dto.AuditDTO;
import com.hospital.integrity.entity.ResearchAchievement;
import com.hospital.integrity.service.AchievementService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 成果接口
 */
@RestController
@RequestMapping("/v1/achievement")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping("/page")
    public Result<PageResult<ResearchAchievement>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String achType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer year) {
        return Result.ok(achievementService.page(pageNum, pageSize, status, achType, keyword, year));
    }

    @PostMapping
    @Log(module = "成果", operation = "新增成果")
    public Result<ResearchAchievement> create(@RequestBody @Valid AchievementDTO dto) {
        return Result.ok(achievementService.create(dto));
    }

    @PutMapping("/{id}")
    @Log(module = "成果", operation = "编辑成果")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid AchievementDTO dto) {
        achievementService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Log(module = "成果", operation = "删除/撤销成果")
    public Result<Void> delete(@PathVariable Long id) {
        achievementService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/submit")
    @Log(module = "成果", operation = "提交成果审核")
    public Result<Void> submit(@PathVariable Long id) {
        achievementService.submit(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @Log(module = "成果", operation = "成果审核")
    public Result<Void> audit(@PathVariable Long id, @RequestBody @Valid AuditDTO dto) {
        achievementService.audit(id, dto);
        return Result.ok();
    }

    @GetMapping("/audit/page")
    public Result<PageResult<ResearchAchievement>> auditPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "ALL") String scope) {
        return Result.ok(achievementService.auditPage(pageNum, pageSize, scope));
    }

    @GetMapping("/{id}")
    public Result<AchievementVO> detail(@PathVariable Long id) {
        return Result.ok(achievementService.detail(id));
    }

    @PostMapping("/{id}/invalidate")
    @Log(module = "成果", operation = "作废成果")
    public Result<Void> invalidate(@PathVariable Long id) {
        achievementService.invalidate(id);
        return Result.ok();
    }

    @GetMapping("/export")
    @Log(module = "成果", operation = "导出成果Excel")
    public void export(@RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String achType,
                       HttpServletResponse response) {
        List<ResearchAchievement> list = achievementService.exportList(status, achType);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=achievements.xlsx");
        try {
            EasyExcel.write(response.getOutputStream(), ExportRow.class)
                    .sheet("科研成果")
                    .doWrite(list.stream().map(ExportRow::of).toList());
        } catch (IOException e) {
            throw new BusinessException("导出失败：" + e.getMessage());
        }
    }

    /** 导出行 */
    @Data
    public static class ExportRow {
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
        @ExcelProperty("得分")
        private String score;
        @ExcelProperty("状态")
        private String status;
        @ExcelProperty("时间")
        private String publishTime;

        static ExportRow of(ResearchAchievement a) {
            ExportRow row = new ExportRow();
            row.setAchType(a.getAchType());
            row.setTitle(a.getTitle());
            row.setAchNo(a.getAchNo());
            row.setSourceName(a.getSourceName());
            row.setLevel(a.getLevel());
            row.setRankInfo(a.getRankInfo());
            row.setScore(a.getScore() == null ? "" : a.getScore().toPlainString());
            row.setStatus(statusLabel(a.getStatus()));
            row.setPublishTime(a.getPublishTime() == null ? ""
                    : a.getPublishTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            return row;
        }

        static String statusLabel(Integer s) {
            return switch (s == null ? -1 : s) {
                case 0 -> "草稿";
                case 1 -> "待科室初审";
                case 2 -> "待科研科终审";
                case 3 -> "已入库";
                case 4 -> "已退回";
                case 5 -> "已撤销";
                case 6 -> "已作废";
                default -> "";
            };
        }
    }
}
