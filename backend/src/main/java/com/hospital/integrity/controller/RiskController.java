package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.dto.CheckConfirmDTO;
import com.hospital.integrity.dto.CheckRecordDTO;
import com.hospital.integrity.dto.CheckVO;
import com.hospital.integrity.dto.ReformDTO;
import com.hospital.integrity.entity.ResearchCheck;
import com.hospital.integrity.entity.ResearchCheckRecord;
import com.hospital.integrity.entity.ResearchRiskLog;
import com.hospital.integrity.entity.ResearchViolation;
import com.hospital.integrity.service.CheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 风险预警与核查处置接口
 */
@RestController
@RequestMapping("/v1/risk")
@RequiredArgsConstructor
public class RiskController {

    private final CheckService checkService;

    // ---------- 预警 ----------

    @GetMapping("/log/page")
    public Result<PageResult<ResearchRiskLog>> riskLogPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskType) {
        return Result.ok(checkService.riskLogPage(pageNum, pageSize, status, riskType));
    }

    @PostMapping("/log/{id}/claim")
    @Log(module = "风控", operation = "认领预警并转工单")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<ResearchCheck> claimRiskLog(@PathVariable Long id) {
        return Result.ok(checkService.claimFromLog(id));
    }

    // ---------- 工单 ----------

    @GetMapping("/check/page")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<PageResult<ResearchCheck>> checkPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        return Result.ok(checkService.checkPage(pageNum, pageSize, status));
    }

    @GetMapping("/check/{id}")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<CheckVO> checkDetail(@PathVariable Long id) {
        return Result.ok(checkService.detail(id));
    }

    @PostMapping("/check/{id}/claim")
    @Log(module = "风控", operation = "认领工单")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<Void> claimCheck(@PathVariable Long id) {
        checkService.claim(id);
        return Result.ok();
    }

    @PostMapping("/check/{id}/record")
    @Log(module = "风控", operation = "新增核查记录")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<Void> addRecord(@PathVariable Long id, @RequestBody @Valid CheckRecordDTO dto) {
        checkService.record(id, dto);
        return Result.ok();
    }

    @PostMapping("/check/{id}/confirm")
    @Log(module = "风控", operation = "失信认定")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<Void> confirm(@PathVariable Long id, @RequestBody CheckConfirmDTO dto) {
        checkService.confirm(id, dto);
        return Result.ok();
    }

    @PostMapping("/check/{id}/dismiss")
    @Log(module = "风控", operation = "误报撤销")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<Void> dismiss(@PathVariable Long id) {
        checkService.dismiss(id);
        return Result.ok();
    }

    @PostMapping("/check/{id}/publish")
    @Log(module = "风控", operation = "发起失信认定公示")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<Void> publish(@PathVariable Long id) {
        checkService.publish(id);
        return Result.ok();
    }

    @PostMapping("/check/{id}/effect")
    @Log(module = "风控", operation = "扣分生效")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<Void> effect(@PathVariable Long id) {
        checkService.effect(id);
        return Result.ok();
    }

    @PostMapping("/check/{id}/archive")
    @Log(module = "风控", operation = "工单归档")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<Void> archive(@PathVariable Long id) {
        checkService.archive(id);
        return Result.ok();
    }

    @GetMapping("/check/{id}/records")
    public Result<List<ResearchCheckRecord>> records(@PathVariable Long id) {
        return Result.ok(checkService.records(id));
    }

    // ---------- 违规 ----------

    @GetMapping("/violation/page")
    public Result<PageResult<ResearchViolation>> violationPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level) {
        return Result.ok(checkService.violationPage(pageNum, pageSize, status, level));
    }

    @PostMapping("/violation/{id}/reform")
    @Log(module = "风控", operation = "提交整改")
    public Result<Void> reform(@PathVariable Long id, @RequestBody @Valid ReformDTO dto) {
        checkService.reform(id, dto.getResult());
        return Result.ok();
    }

    @PostMapping("/violation/{id}/reformCheck")
    @Log(module = "风控", operation = "整改验收")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<Void> reformCheck(@PathVariable Long id, @RequestBody ReformDTO dto) {
        checkService.reformCheck(id, Boolean.TRUE.equals(dto.getPass()), dto.getComment());
        return Result.ok();
    }
}
