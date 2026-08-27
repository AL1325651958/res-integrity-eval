package com.hospital.integrity.controller;

import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.entity.SysLog;
import com.hospital.integrity.entity.SysLoginLog;
import com.hospital.integrity.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 日志审计接口
 */
@RestController
@RequestMapping("/v1/system/log")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SysLogController {

    private final SysLogService logService;

    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "OPERATION") String type,
                          @RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String keyword) {
        if ("LOGIN".equalsIgnoreCase(type)) {
            return Result.ok(logService.loginPage(pageNum, pageSize, keyword));
        }
        return Result.ok(logService.operationPage(pageNum, pageSize, keyword));
    }
}
