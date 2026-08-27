package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.entity.ResearchBlacklist;
import com.hospital.integrity.mapper.ResearchBlacklistMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 黑名单管理（风险期刊/出版社/关键词）
 */
@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final ResearchBlacklistMapper blacklistMapper;

    public PageResult<ResearchBlacklist> page(int pageNum, int pageSize, String blType, String keyword) {
        Page<ResearchBlacklist> page = blacklistMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ResearchBlacklist>()
                        .eq(blType != null && !blType.isBlank(), ResearchBlacklist::getBlType, blType)
                        .like(keyword != null && !keyword.isBlank(), ResearchBlacklist::getBlName, keyword)
                        .orderByDesc(ResearchBlacklist::getCreateTime));
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public void save(ResearchBlacklist blacklist) {
        if (blacklist.getBlId() == null) {
            blacklist.setStatus(blacklist.getStatus() == null ? 1 : blacklist.getStatus());
            blacklistMapper.insert(blacklist);
        } else {
            blacklistMapper.updateById(blacklist);
        }
    }

    public void delete(Long id) {
        blacklistMapper.deleteById(id);
    }
}
