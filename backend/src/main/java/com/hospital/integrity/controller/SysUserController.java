package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.dto.UserDTO;
import com.hospital.integrity.dto.UserVO;
import com.hospital.integrity.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/v1/system/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping("/page")
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long deptId,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(sysUserService.page(pageNum, pageSize, keyword, deptId, status));
    }

    @PostMapping
    @Log(module = "系统", operation = "新增用户")
    public Result<Void> create(@RequestBody @Valid UserDTO dto) {
        sysUserService.create(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @Log(module = "系统", operation = "编辑用户")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UserDTO dto) {
        sysUserService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Log(module = "系统", operation = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @Log(module = "系统", operation = "启用/禁用用户")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        sysUserService.updateStatus(id, body.get("status"));
        return Result.ok();
    }

    @PutMapping("/{id}/resetPwd")
    @Log(module = "系统", operation = "重置用户密码")
    public Result<Void> resetPwd(@PathVariable Long id) {
        sysUserService.resetPwd(id);
        return Result.ok();
    }
}
