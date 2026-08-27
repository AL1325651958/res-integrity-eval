package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.entity.ResearchPublicity;
import com.hospital.integrity.service.PublicityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 公示接口
 */
@RestController
@RequestMapping("/v1/publicity")
@RequiredArgsConstructor
public class PublicityController {

    private final PublicityService publicityService;

    @GetMapping("/page")
    public Result<PageResult<ResearchPublicity>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) String status) {
        return Result.ok(publicityService.page(pageNum, pageSize, status));
    }

    @PostMapping("/{id}/cancel")
    @Log(module = "公示", operation = "撤销公示")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<Void> cancel(@PathVariable Long id) {
        publicityService.cancel(id);
        return Result.ok();
    }
}
