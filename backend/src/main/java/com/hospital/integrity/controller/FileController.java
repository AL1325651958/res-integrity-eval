package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.entity.ResearchAttachment;
import com.hospital.integrity.service.AttachmentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口
 */
@RestController
@RequestMapping("/v1/file")
@RequiredArgsConstructor
public class FileController {

    private final AttachmentService attachmentService;

    @PostMapping("/upload")
    @Log(module = "文件", operation = "上传附件")
    public Result<ResearchAttachment> upload(@RequestParam("file") MultipartFile file,
                                             @RequestParam(defaultValue = "ACH") String bizType,
                                             @RequestParam(required = false) Long bizId) {
        return Result.ok(attachmentService.upload(file, bizType, bizId));
    }

    @GetMapping("/download")
    public void download(@RequestParam Long id, HttpServletResponse response) {
        attachmentService.download(id, response);
    }

    @DeleteMapping("/{id}")
    @Log(module = "文件", operation = "删除附件")
    public Result<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return Result.ok();
    }
}
