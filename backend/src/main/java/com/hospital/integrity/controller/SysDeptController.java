package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.entity.SysDept;
import com.hospital.integrity.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 科室接口
 */
@RestController
@RequestMapping("/v1/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @GetMapping("/tree")
    public Result<List<SysDept>> tree() {
        return Result.ok(deptService.tree());
    }

    @PostMapping
    @Log(module = "系统", operation = "新增科室")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> save(@RequestBody SysDept dept) {
        deptService.save(dept);
        return Result.ok();
    }

    @PutMapping
    @Log(module = "系统", operation = "编辑科室")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@RequestBody SysDept dept) {
        deptService.save(dept);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Log(module = "系统", operation = "删除科室")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}
