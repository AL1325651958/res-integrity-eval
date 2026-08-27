package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.entity.SysRole;
import com.hospital.integrity.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色接口
 */
@RestController
@RequestMapping("/v1/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.ok(roleService.list());
    }

    @PostMapping
    @Log(module = "系统", operation = "新增角色")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> save(@RequestBody SysRole role) {
        roleService.save(role);
        return Result.ok();
    }

    @PutMapping
    @Log(module = "系统", operation = "编辑角色")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@RequestBody SysRole role) {
        roleService.save(role);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Log(module = "系统", operation = "删除角色")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/menus")
    @Log(module = "系统", operation = "角色分配菜单")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> saveRoleMenus(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.saveRoleMenus(id, body.get("menuIds"));
        return Result.ok();
    }

    @GetMapping("/{id}/menus")
    public Result<List<Long>> roleMenuIds(@PathVariable Long id) {
        return Result.ok(roleService.roleMenuIds(id));
    }
}
