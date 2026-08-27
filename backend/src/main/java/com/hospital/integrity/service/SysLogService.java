package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.entity.SysLog;
import com.hospital.integrity.entity.SysLoginLog;
import com.hospital.integrity.mapper.SysLogMapper;
import com.hospital.integrity.mapper.SysLoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 日志审计（只读，日志不可删除）
 */
@Service
@RequiredArgsConstructor
public class SysLogService {

    private final SysLogMapper logMapper;
    private final SysLoginLogMapper loginLogMapper;

    public PageResult<SysLog> operationPage(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(SysLog::getUsername, keyword)
                                .or().like(SysLog::getOperation, keyword)
                                .or().like(SysLog::getModule, keyword))
                .orderByDesc(SysLog::getCreateTime);
        Page<SysLog> page = logMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public PageResult<SysLoginLog> loginPage(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<SysLoginLog>()
                .like(keyword != null && !keyword.isBlank(), SysLoginLog::getUsername, keyword)
                .orderByDesc(SysLoginLog::getLoginTime);
        Page<SysLoginLog> page = loginLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }
}
