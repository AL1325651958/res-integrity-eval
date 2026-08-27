package com.hospital.integrity.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.integrity.entity.ResearchViolation;
import com.hospital.integrity.mapper.ResearchViolationMapper;
import com.hospital.integrity.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 定时任务：每日 09:00 整改到期提醒（整改期限前 3 天）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderTask {

    private final ResearchViolationMapper violationMapper;
    private final NoticeService noticeService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void run() {
        LocalDate soon = LocalDate.now().plusDays(3);
        List<ResearchViolation> list = violationMapper.selectList(new LambdaQueryWrapper<ResearchViolation>()
                .eq(ResearchViolation::getStatus, "REFORMING")
                .le(ResearchViolation::getReformDeadline, soon));
        for (ResearchViolation v : list) {
            noticeService.send(v.getUserId(), "RISK", "整改到期提醒",
                    "您的违规整改期限为 " + v.getReformDeadline() + "，请及时完成整改并提交",
                    "VIOLATION", v.getViolationId());
        }
        if (!list.isEmpty()) {
            log.info("整改到期提醒已发送 {} 条", list.size());
        }
    }
}
