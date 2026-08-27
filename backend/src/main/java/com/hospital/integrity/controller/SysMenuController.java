package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.entity.SysMenu;
import com.hospital.integrity.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单接口
 */
@RestController
@RequestMapping("/v1/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    /** 当前用户菜单树（登录后加载动态路由） */
    @GetMapping("/tree")
    public Result<List<SysMenu>> tree() {
        return Result.ok(menuService.treeForUser());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysMenu>> all() {
        return Result.ok(menuService.treeAll());
    }

    @PostMapping
    @Log(module = "系统", operation = "新增菜单")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> save(@RequestBody SysMenu menu) {
        menuService.save(menu);
        return Result.ok();
    }

    @PutMapping
    @Log(module = "系统", operation = "编辑菜单")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@RequestBody SysMenu menu) {
        menuService.save(menu);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Log(module = "系统", operation = "删除菜单")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
