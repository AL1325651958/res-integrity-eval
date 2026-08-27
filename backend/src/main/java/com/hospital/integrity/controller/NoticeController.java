package com.hospital.integrity.controller;

import com.hospital.integrity.annotation.Log;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.common.Result;
import com.hospital.integrity.entity.ResearchNotice;
import com.hospital.integrity.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知接口
 */
@RestController
@RequestMapping("/v1/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/my")
    public Result<PageResult<ResearchNotice>> my(@RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                 @RequestParam(required = false) Integer isRead) {
        return Result.ok(noticeService.myNotices(pageNum, pageSize, isRead));
    }

    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        noticeService.read(id);
        return Result.ok();
    }

    @PutMapping("/readAll")
    @Log(module = "通知", operation = "全部标记已读")
    public Result<Void> readAll() {
        noticeService.readAll();
        return Result.ok();
    }

    @GetMapping("/unreadCount")
    public Result<Long> unreadCount() {
        return Result.ok(noticeService.unreadCount());
    }
}
