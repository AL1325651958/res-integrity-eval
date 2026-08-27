package com.hospital.integrity.task;

import com.hospital.integrity.service.RiskScreeningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：每日 02:00 自动风控筛查（近 24 小时入库成果）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskScreeningTask {

    private final RiskScreeningService riskScreeningService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void run() {
        log.info("风控筛查定时任务开始");
        riskScreeningService.screenRecent();
        log.info("风控筛查定时任务完成");
    }
}
