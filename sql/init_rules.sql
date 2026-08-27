-- ============================================================
-- 评分规则初始化 V1.0（执行顺序：schema.sql → init_dict.sql → 本脚本）
-- 与 docs/业绩评分规则.md V1.0 保持一致
-- ============================================================
USE `integrity_db`;

-- ============================================================
-- 一、业绩加分规则（rule_type = PERF）
-- ============================================================

-- ---------- 论文（FIXED 基础分，乘作者位次系数） ----------
INSERT INTO `research_rule` (`rule_type`, `rule_no`, `ach_type`, `rule_name`, `base_score`, `calc_mode`, `version`, `effective_date`, `status`, `remark`) VALUES
('PERF', 'PERF-PAPER-01', 'PAPER', 'SCI/SSCI 一区论文', 50, 'FIXED', '1.0', '2025-01-01', 1, '得分=基础分×作者位次系数'),
('PERF', 'PERF-PAPER-02', 'PAPER', 'SCI/SSCI 二区论文', 35, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-PAPER-03', 'PAPER', 'SCI/SSCI 三区论文', 25, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-PAPER-04', 'PAPER', 'SCI/SSCI 四区论文', 15, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-PAPER-05', 'PAPER', 'CSSCI（南大核心）论文', 25, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-PAPER-06', 'PAPER', 'EI期刊/北大中文核心论文', 15, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-PAPER-07', 'PAPER', '科技核心（统计源）论文', 8, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-PAPER-08', 'PAPER', '一般正式期刊论文', 3, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-PAPER-09', 'PAPER', '国际会议论文（EI/CPCI检索）', 5, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-PAPER-10', 'PAPER', '国内会议论文', 2, 'FIXED', '1.0', '2025-01-01', 1, NULL);

-- ---------- 课题（ROLE_MAP 按角色分档） ----------
INSERT INTO `research_rule` (`rule_type`, `rule_no`, `ach_type`, `rule_name`, `base_score`, `calc_mode`, `config_json`, `version`, `effective_date`, `status`, `remark`) VALUES
('PERF', 'PERF-TOPIC-01', 'TOPIC', '国家级课题', NULL, 'ROLE_MAP', JSON_OBJECT('LEADER', 80, 'CORE', 40, 'MEMBER', 16), '1.0', '2025-01-01', 1, '立项年度计60%，结题年度补计40%'),
('PERF', 'PERF-TOPIC-02', 'TOPIC', '省部级课题', NULL, 'ROLE_MAP', JSON_OBJECT('LEADER', 50, 'CORE', 25, 'MEMBER', 10), '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-TOPIC-03', 'TOPIC', '市厅级课题', NULL, 'ROLE_MAP', JSON_OBJECT('LEADER', 25, 'CORE', 12, 'MEMBER', 5), '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-TOPIC-04', 'TOPIC', '院级课题', NULL, 'ROLE_MAP', JSON_OBJECT('LEADER', 8, 'CORE', 4, 'MEMBER', 2), '1.0', '2025-01-01', 1, NULL);

-- ---------- 专利/软著（ROLE_MAP 按发明人排名，1/2/3/4+） ----------
INSERT INTO `research_rule` (`rule_type`, `rule_no`, `ach_type`, `rule_name`, `base_score`, `calc_mode`, `config_json`, `version`, `effective_date`, `status`, `remark`) VALUES
('PERF', 'PERF-PATENT-01', 'PATENT', '发明专利（授权）', NULL, 'ROLE_MAP', JSON_OBJECT('1', 30, '2', 18, '3', 10, '4', 5), '1.0', '2025-01-01', 1, '授权有效计分'),
('PERF', 'PERF-PATENT-02', 'PATENT', '实用新型', NULL, 'ROLE_MAP', JSON_OBJECT('1', 8, '2', 5, '3', 3, '4', 1), '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-PATENT-03', 'PATENT', '软件著作权', NULL, 'ROLE_MAP', JSON_OBJECT('1', 4, '2', 2, '3', 1, '4', 0.5), '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-PATENT-04', 'PATENT', '外观设计', NULL, 'ROLE_MAP', JSON_OBJECT('1', 3, '2', 2, '3', 1, '4', 0.5), '1.0', '2025-01-01', 1, NULL);

-- ---------- 科技奖励（FIXED 基础分=一等奖分值，乘获奖排名系数） ----------
INSERT INTO `research_rule` (`rule_type`, `rule_no`, `ach_type`, `rule_name`, `base_score`, `calc_mode`, `version`, `effective_date`, `status`, `remark`) VALUES
('PERF', 'PERF-REWARD-01', 'REWARD', '国家级一等奖', 120, 'FIXED', '1.0', '2025-01-01', 1, '得分=等级分值×获奖排名系数'),
('PERF', 'PERF-REWARD-02', 'REWARD', '国家级二等奖', 80, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-REWARD-03', 'REWARD', '国家级三等奖', 50, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-REWARD-04', 'REWARD', '省部级一等奖', 60, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-REWARD-05', 'REWARD', '省部级二等奖', 40, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-REWARD-06', 'REWARD', '省部级三等奖', 20, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-REWARD-07', 'REWARD', '市厅级一等奖', 20, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-REWARD-08', 'REWARD', '市厅级二等奖', 12, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-REWARD-09', 'REWARD', '市厅级三等奖', 6, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-REWARD-10', 'REWARD', '院级一等奖', 8, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-REWARD-11', 'REWARD', '院级二等奖', 5, 'FIXED', '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-REWARD-12', 'REWARD', '院级三等奖', 3, 'FIXED', '1.0', '2025-01-01', 1, NULL);

-- ---------- 专著/教材（ROLE_MAP） ----------
INSERT INTO `research_rule` (`rule_type`, `rule_no`, `ach_type`, `rule_name`, `base_score`, `calc_mode`, `config_json`, `version`, `effective_date`, `status`, `remark`) VALUES
('PERF', 'PERF-BOOK-01', 'BOOK', '普通专著/教材', NULL, 'ROLE_MAP', JSON_OBJECT('EDITOR', 30, 'ASSOC', 15, 'CONTRIB', 5), '1.0', '2025-01-01', 1, 'EDITOR主编/ASSOC副主编/CONTRIB编委参编'),
('PERF', 'PERF-BOOK-02', 'BOOK', '国家级规划教材/国家级出版社', NULL, 'ROLE_MAP', JSON_OBJECT('EDITOR', 45, 'ASSOC', 22, 'CONTRIB', 8), '1.0', '2025-01-01', 1, NULL);

-- ---------- 标准/指南（ROLE_MAP） ----------
INSERT INTO `research_rule` (`rule_type`, `rule_no`, `ach_type`, `rule_name`, `base_score`, `calc_mode`, `config_json`, `version`, `effective_date`, `status`, `remark`) VALUES
('PERF', 'PERF-STANDARD-01', 'STANDARD', '国际标准', NULL, 'ROLE_MAP', JSON_OBJECT('MAIN', 60, 'PART', 20), '1.0', '2025-01-01', 1, 'MAIN主要起草人(前3)/PART参与起草'),
('PERF', 'PERF-STANDARD-02', 'STANDARD', '国家标准', NULL, 'ROLE_MAP', JSON_OBJECT('MAIN', 40, 'PART', 15), '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-STANDARD-03', 'STANDARD', '行业标准', NULL, 'ROLE_MAP', JSON_OBJECT('MAIN', 25, 'PART', 10), '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-STANDARD-04', 'STANDARD', '地方/团体标准', NULL, 'ROLE_MAP', JSON_OBJECT('MAIN', 15, 'PART', 5), '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-STANDARD-05', 'STANDARD', '临床指南/专家共识', NULL, 'ROLE_MAP', JSON_OBJECT('MAIN', 20, 'PART', 8), '1.0', '2025-01-01', 1, NULL);

-- ---------- 学术任职（ROLE_MAP） ----------
INSERT INTO `research_rule` (`rule_type`, `rule_no`, `ach_type`, `rule_name`, `base_score`, `calc_mode`, `config_json`, `version`, `effective_date`, `status`, `remark`) VALUES
('PERF', 'PERF-POST-01', 'POST', '国家级学会任职', NULL, 'ROLE_MAP', JSON_OBJECT('CHAIR', 20, 'VICE', 15, 'STANDING', 10, 'MEMBER', 5), '1.0', '2025-01-01', 1, 'CHAIR主任委员/VICE副主任委员/STANDING常务委员/MEMBER委员理事'),
('PERF', 'PERF-POST-02', 'POST', '省级学会任职', NULL, 'ROLE_MAP', JSON_OBJECT('CHAIR', 10, 'VICE', 8, 'STANDING', 5, 'MEMBER', 3), '1.0', '2025-01-01', 1, NULL),
('PERF', 'PERF-POST-03', 'POST', '市级学会任职', NULL, 'ROLE_MAP', JSON_OBJECT('CHAIR', 5, 'VICE', 4, 'STANDING', 3, 'MEMBER', 2), '1.0', '2025-01-01', 1, NULL);

-- ---------- 技术转化（MONEY 金额制） ----------
INSERT INTO `research_rule` (`rule_type`, `rule_no`, `ach_type`, `rule_name`, `base_score`, `calc_mode`, `config_json`, `version`, `effective_date`, `status`, `remark`) VALUES
('PERF', 'PERF-TRANSFER-01', 'TRANSFER', '技术转化到账计分', NULL, 'MONEY', JSON_OBJECT('unit_amount', 100000, 'score_per_unit', 5, 'cap', 100), '1.0', '2025-01-01', 1, '每10万元到账计5分，单成果年度上限100分');

-- ============================================================
-- 二、诚信扣分规则（rule_type = DEDUCT）
-- D 级：一票否决（base_score 置 0，veto_flag=1）
-- B/C 级：固定扣分 + 可整改（need_reform=1）
-- ============================================================
INSERT INTO `research_rule` (`rule_type`, `rule_no`, `ach_type`, `rule_name`, `base_score`, `calc_mode`, `version`, `effective_date`, `status`, `veto_flag`, `need_reform`, `remark`) VALUES
('DEDUCT', 'DEDUCT-D-01', 'PLAGIARISM', '抄袭剽窃', 0, 'FIXED', '1.0', '2025-01-01', 1, 1, 0, '一票否决：年度评价不合格、冻结申报资格2年'),
('DEDUCT', 'DEDUCT-D-02', 'DATA_FAB', '数据造假', 0, 'FIXED', '1.0', '2025-01-01', 1, 1, 0, '一票否决'),
('DEDUCT', 'DEDUCT-D-03', 'EXP_FAB', '实验造假', 0, 'FIXED', '1.0', '2025-01-01', 1, 1, 0, '一票否决'),
('DEDUCT', 'DEDUCT-D-04', 'FORGERY', '伪造证书/立项材料', 0, 'FIXED', '1.0', '2025-01-01', 1, 1, 0, '一票否决'),
('DEDUCT', 'DEDUCT-D-05', 'PAPER_TRADING', '买卖论文', 0, 'FIXED', '1.0', '2025-01-01', 1, 1, 0, '一票否决'),
('DEDUCT', 'DEDUCT-D-06', 'GHOST_WRITE', '代写代发', 0, 'FIXED', '1.0', '2025-01-01', 1, 1, 0, '一票否决'),
('DEDUCT', 'DEDUCT-D-07', 'MALICIOUS', '其他恶意学术不端', 0, 'FIXED', '1.0', '2025-01-01', 1, 1, 0, '一票否决'),
('DEDUCT', 'DEDUCT-C-01', 'MULTI_SUBMIT', '一稿多投', 30, 'FIXED', '1.0', '2025-01-01', 1, 0, 1, '标记风险人员，限制评优评先1年'),
('DEDUCT', 'DEDUCT-C-02', 'DUPLICATE_PUB', '重复发表', 30, 'FIXED', '1.0', '2025-01-01', 1, 0, 1, NULL),
('DEDUCT', 'DEDUCT-C-03', 'MISAPPROPRIATION', '侵占他人成果', 25, 'FIXED', '1.0', '2025-01-01', 1, 0, 1, NULL),
('DEDUCT', 'DEDUCT-C-04', 'IMPROPER_AUTHOR', '不当署名', 20, 'FIXED', '1.0', '2025-01-01', 1, 0, 1, NULL),
('DEDUCT', 'DEDUCT-C-05', 'EXAGGERATION', '成果虚假夸大', 15, 'FIXED', '1.0', '2025-01-01', 1, 0, 1, NULL),
('DEDUCT', 'DEDUCT-B-01', 'DUP_APPLY', '重复申报未及时撤销', 8, 'FIXED', '1.0', '2025-01-01', 1, 0, 1, '限期15日整改'),
('DEDUCT', 'DEDUCT-B-02', 'OMISSION', '填报信息疏漏', 5, 'FIXED', '1.0', '2025-01-01', 1, 0, 1, '限期整改'),
('DEDUCT', 'DEDUCT-B-03', 'INCOMPLETE_ATTACH', '附件不全', 3, 'FIXED', '1.0', '2025-01-01', 1, 0, 1, '限期整改'),
('DEDUCT', 'DEDUCT-B-04', 'MISTAKE', '非主观失误', 2, 'FIXED', '1.0', '2025-01-01', 1, 0, 1, '限期整改');

-- ============================================================
-- 三、计分系数（research_rule_coeff；rule_id 为空=全局通用）
-- ============================================================

-- 作者位次系数（论文通用）
INSERT INTO `research_rule_coeff` (`rule_id`, `coeff_type`, `position_key`, `position_label`, `coefficient`, `sort_order`) VALUES
(NULL, 'RANK', '1ST', '第一作者', 1.00, 1),
(NULL, 'RANK', 'CORRESP', '通讯作者', 1.00, 2),
(NULL, 'RANK', '2ND', '第二作者', 0.60, 3),
(NULL, 'RANK', '3RD', '第三作者', 0.40, 4),
(NULL, 'RANK', '4TH', '第四作者', 0.25, 5),
(NULL, 'RANK', '5TH+', '第五作者及以后', 0.10, 6);

-- 获奖排名系数（科技奖励通用）
INSERT INTO `research_rule_coeff` (`rule_id`, `coeff_type`, `position_key`, `position_label`, `coefficient`, `sort_order`) VALUES
(NULL, 'REWARD_RANK', '1', '第1名', 1.00, 1),
(NULL, 'REWARD_RANK', '2', '第2名', 0.60, 2),
(NULL, 'REWARD_RANK', '3', '第3名', 0.40, 3),
(NULL, 'REWARD_RANK', '4+', '第4名及以后', 0.20, 4);

-- ============================================================
-- 四、诚信等级判定阈值（research_level_config）
-- ============================================================
INSERT INTO `research_level_config` (`level`, `level_name`, `min_deduct`, `max_deduct`, `veto_flag`, `conditions`, `version`, `effective_date`, `status`) VALUES
('A', '诚信优秀', 0, 0, 0, '年度有效扣分=0 且无任何风险/违规记录', '1.0', '2025-01-01', 1),
('B', '诚信合格', 0, 10, 0, '0<有效扣分≤10 且所有B级违规已完成整改', '1.0', '2025-01-01', 1),
('C', '诚信警示', 10, 30, 0, '10<有效扣分≤30，或存在未完成整改的B/C级违规', '1.0', '2025-01-01', 1),
('D', '严重失信', 30, 999999, 1, '存在任一票否决项，或有效扣分>30，或同年度≥2项C级违规', '1.0', '2025-01-01', 1);
