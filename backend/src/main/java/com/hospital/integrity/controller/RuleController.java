package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.entity.ResearchLevelConfig;
import com.hospital.integrity.entity.ResearchRule;
import com.hospital.integrity.entity.ResearchRuleCoeff;
import com.hospital.integrity.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评分规则接口
 */
@RestController
@RequestMapping("/v1/system/rule")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @GetMapping("/page")
    public Result<PageResult<ResearchRule>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) String ruleType,
                                                 @RequestParam(required = false) String achType) {
        return Result.ok(ruleService.page(pageNum, pageSize, ruleType, achType));
    }

    @PostMapping
    @Log(module = "规则", operation = "新增评分规则")
    @PreAuthorize("hasAnyRole('COMMITTEE','ADMIN')")
    public Result<Void> save(@RequestBody ResearchRule rule) {
        ruleService.save(rule);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @Log(module = "规则", operation = "编辑评分规则")
    @PreAuthorize("hasAnyRole('COMMITTEE','ADMIN')")
    public Result<Void> update(@PathVariable Long id, @RequestBody ResearchRule rule) {
        rule.setRuleId(id);
        ruleService.save(rule);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Log(module = "规则", operation = "删除评分规则")
    @PreAuthorize("hasAnyRole('COMMITTEE','ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        ruleService.delete(id);
        return Result.ok();
    }

    @GetMapping("/coeff/list")
    public Result<List<ResearchRuleCoeff>> coeffList() {
        return Result.ok(ruleService.coeffList());
    }

    @GetMapping("/level/list")
    public Result<List<ResearchLevelConfig>> levelList() {
        return Result.ok(ruleService.levelList());
    }
}
