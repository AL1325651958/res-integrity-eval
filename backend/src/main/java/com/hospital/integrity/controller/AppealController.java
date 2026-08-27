package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.dto.AppealDTO;
import com.hospital.integrity.dto.AppealReviewDTO;
import com.hospital.integrity.entity.ResearchAppeal;
import com.hospital.integrity.service.AppealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 申诉接口
 */
@RestController
@RequestMapping("/v1/appeal")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;

    @PostMapping
    @Log(module = "申诉", operation = "提交申诉")
    public Result<ResearchAppeal> create(@RequestBody @Valid AppealDTO dto) {
        return Result.ok(appealService.create(dto));
    }

    @GetMapping("/page")
    public Result<PageResult<ResearchAppeal>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        return Result.ok(appealService.page(pageNum, pageSize, status));
    }

    @GetMapping("/{id}")
    public Result<ResearchAppeal> detail(@PathVariable Long id) {
        return Result.ok(appealService.detail(id));
    }

    @PostMapping("/{id}/review")
    @Log(module = "申诉", operation = "申诉复核裁定")
    @PreAuthorize("hasAnyRole('COMMITTEE','ADMIN')")
    public Result<Void> review(@PathVariable Long id, @RequestBody @Valid AppealReviewDTO dto) {
        appealService.review(id, dto.getPass(), dto.getResult());
        return Result.ok();
    }
}
