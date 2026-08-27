package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.entity.ResearchNotice;
import com.hospital.integrity.mapper.ResearchNoticeMapper;
import com.hospital.integrity.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 站内通知
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final ResearchNoticeMapper noticeMapper;

    public void send(Long userId, String noticeType, String title, String content, String bizType, Long bizId) {
        ResearchNotice notice = new ResearchNotice();
        notice.setUserId(userId);
        notice.setNoticeType(noticeType);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setBizType(bizType);
        notice.setBizId(bizId);
        notice.setIsRead(0);
        noticeMapper.insert(notice);
    }

    public PageResult<ResearchNotice> myNotices(int pageNum, int pageSize, Integer isRead) {
        Page<ResearchNotice> page = noticeMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ResearchNotice>()
                        .eq(ResearchNotice::getUserId, SecurityUtils.currentUserId())
                        .eq(isRead != null, ResearchNotice::getIsRead, isRead)
                        .orderByDesc(ResearchNotice::getCreateTime));
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public void read(Long id) {
        noticeMapper.update(null, new LambdaUpdateWrapper<ResearchNotice>()
                .eq(ResearchNotice::getNoticeId, id)
                .eq(ResearchNotice::getUserId, SecurityUtils.currentUserId())
                .set(ResearchNotice::getIsRead, 1));
    }

    public void readAll() {
        noticeMapper.update(null, new LambdaUpdateWrapper<ResearchNotice>()
                .eq(ResearchNotice::getUserId, SecurityUtils.currentUserId())
                .set(ResearchNotice::getIsRead, 1));
    }

    public long unreadCount() {
        return noticeMapper.selectCount(new LambdaQueryWrapper<ResearchNotice>()
                .eq(ResearchNotice::getUserId, SecurityUtils.currentUserId())
                .eq(ResearchNotice::getIsRead, 0));
    }
}
