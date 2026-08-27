-- ============================================================
-- 字典数据初始化 V1.0（执行顺序：先 schema.sql 后本脚本）
-- ============================================================
USE `integrity_db`;

-- ---------- 字典类型 ----------
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `remark`) VALUES
('成果类型', 'ach_type', 1, '科研成果大类'),
('违规等级', 'violation_level', 1, 'B轻微 C中度 D严重'),
('违规类型', 'violation_type', 1, '学术不端违规情形'),
('风险类型', 'risk_type', 1, '自动风控筛查类型'),
('核查工单状态', 'check_status', 1, '失信核查工单生命周期'),
('申诉状态', 'appeal_status', 1, '申诉流程状态'),
('通知类型', 'notice_type', 1, '站内通知类型'),
('公示类型', 'publicity_type', 1, '公示对象类型'),
('黑名单类型', 'bl_type', 1, '期刊/出版社/关键词'),
('审核操作类型', 'audit_type', 1, '成果审核流水操作'),
('系数类型', 'coeff_type', 1, '计分系数分类');

-- ---------- 成果类型 ach_type ----------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`) VALUES
('ach_type', '期刊论文/会议论文', 'PAPER', 1),
('ach_type', '课题项目', 'TOPIC', 2),
('ach_type', '专利/软著', 'PATENT', 3),
('ach_type', '科技奖励', 'REWARD', 4),
('ach_type', '专著/教材', 'BOOK', 5),
('ach_type', '标准/指南', 'STANDARD', 6),
('ach_type', '学术任职', 'POST', 7),
('ach_type', '技术转化', 'TRANSFER', 8),
('ach_type', '其他成果', 'OTHER', 9);

-- ---------- 违规等级 violation_level ----------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`) VALUES
('violation_level', '轻微失信', 'B', 1),
('violation_level', '中度失信', 'C', 2),
('violation_level', '严重失信', 'D', 3);

-- ---------- 违规类型 violation_type ----------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`) VALUES
('violation_type', '抄袭剽窃', 'PLAGIARISM', 1),
('violation_type', '数据造假', 'DATA_FAB', 2),
('violation_type', '实验造假', 'EXP_FAB', 3),
('violation_type', '伪造证书/立项材料', 'FORGERY', 4),
('violation_type', '买卖论文', 'PAPER_TRADING', 5),
('violation_type', '代写代发', 'GHOST_WRITE', 6),
('violation_type', '一稿多投', 'MULTI_SUBMIT', 7),
('violation_type', '重复发表', 'DUPLICATE_PUB', 8),
('violation_type', '不当署名', 'IMPROPER_AUTHOR', 9),
('violation_type', '侵占他人成果', 'MISAPPROPRIATION', 10),
('violation_type', '成果虚假夸大', 'EXAGGERATION', 11),
('violation_type', '重复申报未及时撤销', 'DUP_APPLY', 12),
('violation_type', '填报信息疏漏', 'OMISSION', 13),
('violation_type', '附件不全', 'INCOMPLETE_ATTACH', 14),
('violation_type', '非主观失误', 'MISTAKE', 15),
('violation_type', '其他恶意学术不端', 'MALICIOUS', 16);

-- ---------- 风险类型 risk_type ----------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`) VALUES
('risk_type', '一稿多投预警', 'MULTI_SUBMIT', 1),
('risk_type', '署名异常预警', 'AUTHOR_ANOMALY', 2),
('risk_type', '时间逻辑预警', 'TIME_LOGIC', 3),
('risk_type', '黑名单期刊预警', 'BLACKLIST_JOURNAL', 4),
('risk_type', '成果重复申报预警', 'DUP_APPLY', 5),
('risk_type', '其他风险', 'OTHER', 6);

-- ---------- 核查工单状态 check_status ----------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`) VALUES
('check_status', '待认领', 'PENDING', 1),
('check_status', '核查中', 'PROCESSING', 2),
('check_status', '待认定', 'TO_CONFIRM', 3),
('check_status', '已认定', 'CONFIRMED', 4),
('check_status', '待公示', 'TO_PUBLIC', 5),
('check_status', '已生效', 'PUBLISHED', 6),
('check_status', '已归档', 'ARCHIVED', 7),
('check_status', '误报撤销', 'DISMISSED', 8);

-- ---------- 申诉状态 appeal_status ----------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`) VALUES
('appeal_status', '待受理', 'PENDING', 1),
('appeal_status', '复核中', 'REVIEWING', 2),
('appeal_status', '维持原判', 'SUSTAINED', 3),
('appeal_status', '变更裁定', 'OVERTURNED', 4),
('appeal_status', '驳回', 'REJECTED', 5);

-- ---------- 通知类型 notice_type ----------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`) VALUES
('notice_type', '风险预警', 'RISK', 1),
('notice_type', '审核通知', 'AUDIT', 2),
('notice_type', '申诉通知', 'APPEAL', 3),
('notice_type', '评价通知', 'EVALUATE', 4),
('notice_type', '系统通知', 'SYSTEM', 5);

-- ---------- 公示类型 publicity_type ----------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`) VALUES
('publicity_type', '评价结果公示', 'EVALUATE', 1),
('publicity_type', '失信认定公示', 'CONFIRM', 2);

-- ---------- 黑名单类型 bl_type ----------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`) VALUES
('bl_type', '风险期刊', 'JOURNAL', 1),
('bl_type', '风险出版社', 'PUBLISHER', 2),
('bl_type', '违规关键词', 'KEYWORD', 3);

-- ---------- 审核操作类型 audit_type ----------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`) VALUES
('audit_type', '提交审核', 'SUBMIT', 1),
('audit_type', '退回修改', 'BACK', 2),
('audit_type', '终审通过', 'APPROVE', 3),
('audit_type', '撤销', 'CANCEL', 4),
('audit_type', '作废', 'INVALID', 5);

-- ---------- 系数类型 coeff_type ----------
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`) VALUES
('coeff_type', '作者位次系数', 'RANK', 1),
('coeff_type', '课题角色系数', 'TOPIC_ROLE', 2),
('coeff_type', '获奖排名系数', 'REWARD_RANK', 3);
