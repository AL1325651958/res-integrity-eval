package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.dto.PublicityDTO;
import com.hospital.integrity.entity.ResearchPublicity;
import com.hospital.integrity.mapper.ResearchPublicityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 公示管理：评价结果公示、失信认定公示
 */
@Service
@RequiredArgsConstructor
public class PublicityService {

    private final ResearchPublicityMapper publicityMapper;

    public ResearchPublicity create(PublicityDTO dto, String publicityType, String bizType, Long bizId) {
        ResearchPublicity p = new ResearchPublicity();
        p.setPublicityNo(genNo());
        p.setPublicityType(publicityType);
        p.setBizType(bizType);
        p.setBizIds("[" + bizId + "]");
        p.setScope(dto.getScope() == null ? "ALL" : dto.getScope());
        p.setStartTime(dto.getStartTime() == null ? LocalDateTime.now() : dto.getStartTime());
        p.setEndTime(dto.getEndTime() == null ? LocalDateTime.now().plusDays(5) : dto.getEndTime());
        p.setStatus("PUBLISHING");
        p.setCreateBy(com.hospital.integrity.security.SecurityUtils.currentUserId());
        publicityMapper.insert(p);
        return p;
    }

    public PageResult<ResearchPublicity> page(int pageNum, int pageSize, String status) {
        Page<ResearchPublicity> page = publicityMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ResearchPublicity>()
                        .eq(status != null && !status.isBlank(), ResearchPublicity::getStatus, status)
                        .orderByDesc(ResearchPublicity::getCreateTime));
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public void cancel(Long id) {
        ResearchPublicity p = publicityMapper.selectById(id);
        if (p == null) {
            throw new BusinessException("公示记录不存在");
        }
        p.setStatus("CANCELED");
        publicityMapper.updateById(p);
    }

    private String genNo() {
        return "GB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (int) (Math.random() * 90 + 10);
    }
}
