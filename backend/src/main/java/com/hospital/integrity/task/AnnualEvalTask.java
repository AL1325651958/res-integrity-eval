package com.hospital.integrity.task;

import com.hospital.integrity.service.IntegrityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 定时任务：年度诚信评价计算（每年 1 月 5 日 02:30 计算上年度）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnualEvalTask {

    private final IntegrityService integrityService;

    @Scheduled(cron = "0 30 2 5 1 ?")
    public void run() {
        int year = LocalDate.now().getYear() - 1;
        log.info("年度评价定时任务开始：year={}", year);
        int count = integrityService.calcYear(year);
        log.info("年度评价定时任务完成：year={}, 计算{}人", year, count);
    }
}
