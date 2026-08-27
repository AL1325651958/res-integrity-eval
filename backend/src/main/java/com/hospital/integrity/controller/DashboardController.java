package com.hospital.integrity.controller;

import com.hospital.integrity.common.Result;
import com.hospital.integrity.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 看板接口
 */
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/my")
    public Result<Map<String, Object>> my() {
        return Result.ok(dashboardService.my());
    }

    @GetMapping("/dept")
    @PreAuthorize("hasAnyRole('DEPT_ADMIN','AUDITOR','LEADER','ADMIN')")
    public Result<Map<String, Object>> dept() {
        return Result.ok(dashboardService.dept());
    }

    @GetMapping("/hospital")
    @PreAuthorize("hasAnyRole('LEADER','ADMIN')")
    public Result<Map<String, Object>> hospital() {
        return Result.ok(dashboardService.hospital());
    }
}
