package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.entity.ResearchBlacklist;
import com.hospital.integrity.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 黑名单接口
 */
@RestController
@RequestMapping("/v1/system/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistService blacklistService;

    @GetMapping("/page")
    public Result<PageResult<ResearchBlacklist>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) String blType,
                                                      @RequestParam(required = false) String keyword) {
        return Result.ok(blacklistService.page(pageNum, pageSize, blType, keyword));
    }

    @PostMapping
    @Log(module = "黑名单", operation = "新增黑名单")
    @PreAuthorize("hasAnyRole('COMMITTEE','ADMIN')")
    public Result<Void> save(@RequestBody ResearchBlacklist blacklist) {
        blacklistService.save(blacklist);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @Log(module = "黑名单", operation = "编辑黑名单")
    @PreAuthorize("hasAnyRole('COMMITTEE','ADMIN')")
    public Result<Void> update(@PathVariable Long id, @RequestBody ResearchBlacklist blacklist) {
        blacklist.setBlId(id);
        blacklistService.save(blacklist);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Log(module = "黑名单", operation = "删除黑名单")
    @PreAuthorize("hasAnyRole('COMMITTEE','ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        blacklistService.delete(id);
        return Result.ok();
    }
}
