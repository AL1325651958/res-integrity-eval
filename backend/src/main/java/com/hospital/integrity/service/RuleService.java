package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospital.integrity.common.BusinessException;
import com.hospital.integrity.common.PageResult;
import com.hospital.integrity.entity.ResearchLevelConfig;
import com.hospital.integrity.entity.ResearchRule;
import com.hospital.integrity.entity.ResearchRuleCoeff;
import com.hospital.integrity.mapper.ResearchLevelConfigMapper;
import com.hospital.integrity.mapper.ResearchRuleCoeffMapper;
import com.hospital.integrity.mapper.ResearchRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评分规则配置（业绩/扣分/系数/等级阈值）
 */
@Service
@RequiredArgsConstructor
public class RuleService {

    private final ResearchRuleMapper ruleMapper;
    private final ResearchRuleCoeffMapper coeffMapper;
    private final ResearchLevelConfigMapper levelConfigMapper;

    public PageResult<ResearchRule> page(int pageNum, int pageSize, String ruleType, String achType) {
        Page<ResearchRule> page = ruleMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ResearchRule>()
                        .eq(ruleType != null && !ruleType.isBlank(), ResearchRule::getRuleType, ruleType)
                        .eq(achType != null && !achType.isBlank(), ResearchRule::getAchType, achType)
                        .orderByAsc(ResearchRule::getRuleNo));
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    public void save(ResearchRule rule) {
        if (rule.getRuleId() == null) {
            Long exists = ruleMapper.selectCount(new LambdaQueryWrapper<ResearchRule>()
                    .eq(ResearchRule::getRuleNo, rule.getRuleNo()));
            if (exists != null && exists > 0) {
                throw new BusinessException("规则编号已存在");
            }
            if (rule.getVersion() == null || rule.getVersion().isBlank()) {
                rule.setVersion("1.0");
            }
            if (rule.getEffectiveDate() == null) {
                rule.setEffectiveDate(java.time.LocalDate.now());
            }
            rule.setStatus(rule.getStatus() == null ? 1 : rule.getStatus());
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
    }

    public void delete(Long id) {
        ruleMapper.deleteById(id);
    }

    public List<ResearchRuleCoeff> coeffList() {
        return coeffMapper.selectList(new LambdaQueryWrapper<ResearchRuleCoeff>()
                .orderByAsc(ResearchRuleCoeff::getCoeffType)
                .orderByAsc(ResearchRuleCoeff::getSortOrder));
    }

    public List<ResearchLevelConfig> levelList() {
        return levelConfigMapper.selectList(new LambdaQueryWrapper<ResearchLevelConfig>()
                .eq(ResearchLevelConfig::getStatus, 1)
                .orderByAsc(ResearchLevelConfig::getLevel));
    }
}
