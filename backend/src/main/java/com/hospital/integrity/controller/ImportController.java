package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.service.ExcelImportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 数据导入接口
 */
@RestController
@RequestMapping("/v1/import")
@RequiredArgsConstructor
public class ImportController {

    private final ExcelImportService importService;

    @PostMapping("/achievement")
    @Log(module = "导入", operation = "Excel批量导入成果")
    @PreAuthorize("hasAnyRole('AUDITOR','ADMIN')")
    public Result<Map<String, Object>> importAchievements(@RequestParam("file") MultipartFile file) {
        return Result.ok(importService.importAchievements(file));
    }

    @GetMapping("/template")
    public void template(HttpServletResponse response) {
        importService.downloadTemplate(response);
    }
}
