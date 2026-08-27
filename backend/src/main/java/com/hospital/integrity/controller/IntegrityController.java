package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.dto.PublicityDTO;
import com.hospital.integrity.entity.ResearchIntegrity;
import com.hospital.integrity.entity.ResearchIntegrityDetail;
import com.hospital.integrity.service.IntegrityService;
import com.hospital.integrity.service.PdfExportService;
import com.hospital.integrity.service.PublicityService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 诚信评价接口
 */
@RestController
@RequestMapping("/v1/integrity")
@RequiredArgsConstructor
public class IntegrityController {

    private final IntegrityService integrityService;
    private final PublicityService publicityService;
    private final PdfExportService pdfExportService;

    @GetMapping("/my")
    public Result<ResearchIntegrity> my(@RequestParam(required = false) Integer year) {
        return Result.ok(integrityService.myIntegrity(year));
    }

    @GetMapping("/my/detail")
    public Result<List<ResearchIntegrityDetail>> myDetail(@RequestParam(required = false) Integer year) {
        return Result.ok(integrityService.myDetail(year));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','LEADER','ADMIN')")
    public Result<PageResult<IntegrityService.IntegrityRow>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(integrityService.list(pageNum, pageSize, year, level, deptId, keyword));
    }

    @PostMapping("/calc")
    @Log(module = "评价", operation = "触发年度评价计算")
    @PreAuthorize("hasAnyRole('AUDITOR','ADMIN')")
    public Result<Integer> calc(@RequestParam(required = false) Integer year) {
        int y = year == null ? java.time.LocalDate.now().getYear() - 1 : year;
        return Result.ok(integrityService.calcYear(y));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','LEADER','ADMIN')")
    public Result<ResearchIntegrity> userIntegrity(@PathVariable Long userId,
                                                   @RequestParam(required = false) Integer year) {
        return Result.ok(integrityService.userIntegrity(userId, year));
    }

    @GetMapping("/user/{userId}/detail")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','LEADER','ADMIN')")
    public Result<java.util.Map<String, Object>> userDetail(@PathVariable Long userId,
                                                            @RequestParam(required = false) Integer year) {
        return Result.ok(integrityService.userDetail(userId, year));
    }

    @GetMapping("/export/pdf")
    public void exportPdf(@RequestParam Long userId, @RequestParam(required = false) Integer year,
                          HttpServletResponse response) {
        pdfExportService.exportArchive(userId, year, response);
    }

    @PostMapping("/{id}/publicity")
    @Log(module = "评价", operation = "发起评价结果公示")
    @PreAuthorize("hasAnyRole('AUDITOR','COMMITTEE','ADMIN')")
    public Result<Void> publicity(@PathVariable Long id, @RequestBody PublicityDTO dto) {
        integrityService.checkExists(id);
        publicityService.create(dto, "EVALUATE", "INTEGRITY", id);
        return Result.ok();
    }
}
