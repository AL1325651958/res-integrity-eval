package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.dto.LoginDTO;
import com.hospital.integrity.dto.LoginVO;
import com.hospital.integrity.dto.PasswordDTO;
import com.hospital.integrity.dto.UserInfoVO;
import com.hospital.integrity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.ok(authService.info());
    }

    @PostMapping("/logout")
    @Log(module = "认证", operation = "退出登录")
    public Result<Void> logout() {
        return Result.ok();
    }

    @PutMapping("/password")
    @Log(module = "认证", operation = "修改密码")
    public Result<Void> changePassword(@RequestBody @Valid PasswordDTO dto) {
        authService.changePassword(dto);
        return Result.ok();
    }
}
