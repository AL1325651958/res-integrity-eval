package com.hospital.integrity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.integrity.entity.ResearchAchievement;
import com.hospital.integrity.entity.ResearchRule;
import com.hospital.integrity.entity.ResearchRuleCoeff;
import com.hospital.integrity.mapper.ResearchRuleCoeffMapper;
import com.hospital.integrity.mapper.ResearchRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 评分引擎：按成果类型/级别/位次匹配规则与系数计算业绩得分。
 * 规则匹配约定与 docs/业绩评分规则.md、sql/init_rules.sql 一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreEngine {

    private final ResearchRuleMapper ruleMapper;
    private final ResearchRuleCoeffMapper coeffMapper;
    private final ObjectMapper objectMapper;

    /** 违规规则信息（扣分表） */
    public record DeductRule(BigDecimal deductScore, boolean veto, boolean needReform) {
    }

    /**
     * 计算单条成果业绩得分；未匹配到规则返回 null（不计分）
     */
    public BigDecimal calcScore(ResearchAchievement ach) {
        String type = ach.getAchType();
        if (type == null) {
            return null;
        }
        String ruleNo = matchRuleNo(type, ach.getLevel());
        if (ruleNo == null && "TRANSFER".equals(type)) {
            ruleNo = "PERF-TRANSFER-01";
        }
        if (ruleNo == null) {
            log.debug("成果[{}]未匹配到规则: type={}, level={}", ach.getTitle(), type, ach.getLevel());
            return null;
        }
        ResearchRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<ResearchRule>()
                .eq(ResearchRule::getRuleNo, ruleNo)
                .eq(ResearchRule::getStatus, 1)
                .last("LIMIT 1"));
        if (rule == null) {
            return null;
        }
        return switch (rule.getCalcMode() == null ? "FIXED" : rule.getCalcMode()) {
            case "FIXED" -> rule.getBaseScore();
            case "COEFF" -> {
                BigDecimal base = rule.getBaseScore();
                BigDecimal coeff = coeffFor(type, ach.getRankInfo(), ach.getIsCorresponding());
                yield (base != null && coeff != null)
                        ? base.multiply(coeff).setScale(2, RoundingMode.HALF_UP)
                        : null;
            }
            case "ROLE_MAP" -> {
                Map<String, BigDecimal> config = parseConfig(rule.getConfigJson());
                String key = roleKeyFor(type, ach.getRankInfo());
                yield (config != null && key != null) ? config.get(key) : null;
            }
            case "MONEY" -> {
                Map<String, BigDecimal> config = parseConfig(rule.getConfigJson());
                if (config == null || ach.getFundAmount() == null) {
                    yield null;
                }
                BigDecimal unit = config.getOrDefault("unit_amount", BigDecimal.valueOf(100000));
                BigDecimal per = config.getOrDefault("score_per_unit", BigDecimal.valueOf(5));
                BigDecimal cap = config.getOrDefault("cap", BigDecimal.valueOf(100));
                BigDecimal score = ach.getFundAmount().divide(unit, 4, RoundingMode.HALF_UP).multiply(per);
                yield score.min(cap).setScale(2, RoundingMode.HALF_UP);
            }
            default -> null;
        };
    }

    /**
     * 按违规类型查扣分规则（DEDUCT 表）
     */
    public DeductRule deductRule(String violationType) {
        ResearchRule rule = ruleMapper.selectOne(new LambdaQueryWrapper<ResearchRule>()
                .eq(ResearchRule::getRuleType, "DEDUCT")
                .eq(ResearchRule::getAchType, violationType)
                .eq(ResearchRule::getStatus, 1)
                .last("LIMIT 1"));
        if (rule == null) {
            return new DeductRule(BigDecimal.ZERO, false, false);
        }
        boolean veto = rule.getVetoFlag() != null && rule.getVetoFlag() == 1;
        boolean needReform = rule.getNeedReform() != null && rule.getNeedReform() == 1;
        return new DeductRule(rule.getBaseScore() == null ? BigDecimal.ZERO : rule.getBaseScore(), veto, needReform);
    }

    // ---------------- 规则匹配 ----------------

    private String matchRuleNo(String type, String level) {
        if (level == null) {
            return null;
        }
        String lv = level.trim();
        return switch (type) {
            case "PAPER" -> switch (lv) {
                case "SCI-1区" -> "PERF-PAPER-01";
                case "SCI-2区" -> "PERF-PAPER-02";
                case "SCI-3区" -> "PERF-PAPER-03";
                case "SCI-4区" -> "PERF-PAPER-04";
                case "CSSCI" -> "PERF-PAPER-05";
                case "EI/北大核心" -> "PERF-PAPER-06";
                case "科技核心" -> "PERF-PAPER-07";
                case "一般期刊" -> "PERF-PAPER-08";
                case "国际会议" -> "PERF-PAPER-09";
                case "国内会议" -> "PERF-PAPER-10";
                default -> null;
            };
            case "TOPIC" -> switch (lv) {
                case "国家级" -> "PERF-TOPIC-01";
                case "省部级" -> "PERF-TOPIC-02";
                case "市厅级" -> "PERF-TOPIC-03";
                case "院级" -> "PERF-TOPIC-04";
                default -> null;
            };
            case "PATENT" -> switch (lv) {
                case "发明专利" -> "PERF-PATENT-01";
                case "实用新型" -> "PERF-PATENT-02";
                case "软著" -> "PERF-PATENT-03";
                case "外观设计" -> "PERF-PATENT-04";
                default -> null;
            };
            case "REWARD" -> {
                String p = lv.contains("一等奖") ? "01" : lv.contains("二等奖") ? "02" : lv.contains("三等奖") ? "03" : null;
                if (p == null) {
                    yield null;
                }
                if (lv.contains("国家")) {
                    yield "PERF-REWARD-" + p;
                } else if (lv.contains("省")) {
                    yield "PERF-REWARD-" + (Integer.parseInt(p) + 3);
                } else if (lv.contains("市")) {
                    yield "PERF-REWARD-" + (Integer.parseInt(p) + 6);
                } else if (lv.contains("院")) {
                    yield "PERF-REWARD-" + (Integer.parseInt(p) + 9);
                }
                yield null;
            }
            case "BOOK" -> lv.contains("国家") ? "PERF-BOOK-02" : "PERF-BOOK-01";
            case "STANDARD" -> {
                if (lv.contains("国际")) {
                    yield "PERF-STANDARD-01";
                } else if (lv.contains("国家")) {
                    yield "PERF-STANDARD-02";
                } else if (lv.contains("行业")) {
                    yield "PERF-STANDARD-03";
                } else if (lv.contains("地方") || lv.contains("团体")) {
                    yield "PERF-STANDARD-04";
                } else if (lv.contains("指南") || lv.contains("共识")) {
                    yield "PERF-STANDARD-05";
                }
                yield null;
            }
            case "POST" -> {
                if (lv.contains("国家")) {
                    yield "PERF-POST-01";
                } else if (lv.contains("省")) {
                    yield "PERF-POST-02";
                } else if (lv.contains("市")) {
                    yield "PERF-POST-03";
                }
                yield null;
            }
            default -> null;
        };
    }

    // ---------------- 系数 / 角色档位 ----------------

    private BigDecimal coeffFor(String type, String rankInfo, Integer isCorresponding) {
        String coeffType = "REWARD".equals(type) ? "REWARD_RANK" : "RANK";
        String key;
        if ("PAPER".equals(type) && isCorresponding != null && isCorresponding == 1) {
            key = "CORRESP";
        } else {
            key = rankKey(coeffType, rankInfo);
        }
        if (key == null) {
            return null;
        }
        ResearchRuleCoeff coeff = coeffMapper.selectOne(new LambdaQueryWrapper<ResearchRuleCoeff>()
                .eq(ResearchRuleCoeff::getCoeffType, coeffType)
                .eq(ResearchRuleCoeff::getPositionKey, key)
                .isNull(ResearchRuleCoeff::getRuleId)
                .last("LIMIT 1"));
        return coeff == null ? null : coeff.getCoefficient();
    }

    private String rankKey(String coeffType, String rankInfo) {
        if (rankInfo == null) {
            return null;
        }
        int n = firstDigit(rankInfo);
        if ("REWARD_RANK".equals(coeffType)) {
            return n <= 0 ? null : (n >= 4 ? "4+" : String.valueOf(n));
        }
        // RANK 作者位次
        return switch (n) {
            case 1 -> "1ST";
            case 2 -> "2ND";
            case 3 -> "3RD";
            case 4 -> "4TH";
            default -> n >= 5 ? "5TH+" : null;
        };
    }

    private String roleKeyFor(String type, String rankInfo) {
        if (rankInfo == null) {
            return null;
        }
        return switch (type) {
            case "TOPIC" -> {
                if (rankInfo.contains("负责人")) {
                    yield "LEADER";
                } else if (rankInfo.contains("核心")) {
                    yield "CORE";
                } else if (rankInfo.contains("参与")) {
                    yield "MEMBER";
                }
                yield null;
            }
            case "PATENT" -> {
                int n = firstDigit(rankInfo);
                yield n <= 0 ? null : (n >= 4 ? "4" : String.valueOf(n));
            }
            case "BOOK" -> {
                if (rankInfo.contains("主编")) {
                    yield "EDITOR";
                } else if (rankInfo.contains("副主编")) {
                    yield "ASSOC";
                } else if (rankInfo.contains("编委") || rankInfo.contains("参编")) {
                    yield "CONTRIB";
                }
                yield null;
            }
            case "STANDARD" -> rankInfo.contains("主要") ? "MAIN" : rankInfo.contains("参与") ? "PART" : null;
            case "POST" -> {
                if (rankInfo.contains("主任委员") || rankInfo.contains("会长")) {
                    yield "CHAIR";
                } else if (rankInfo.contains("副主任委员") || rankInfo.contains("副会长")) {
                    yield "VICE";
                } else if (rankInfo.contains("常务委员")) {
                    yield "STANDING";
                } else if (rankInfo.contains("委员") || rankInfo.contains("理事")) {
                    yield "MEMBER";
                }
                yield null;
            }
            default -> null;
        };
    }

    private int firstDigit(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                return Character.digit(c, 10);
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> parseConfig(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("规则配置解析失败: {}", json);
            return null;
        }
    }
}
