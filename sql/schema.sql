-- ============================================================
-- 医院个人科研成果诚信综合评价系统 数据库脚本 V1.0
-- 数据库：MySQL 8.0 / InnoDB / utf8mb4
-- 说明：采用逻辑外键（应用层保证完整性），不建物理 FOREIGN KEY，
--       便于数据初始化与后续演进；金额/分值统一 DECIMAL。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `integrity_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `integrity_db`;

-- ------------------------------------------------------------
-- 1. 科室表 sys_dept
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dept` (
  `dept_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '科室ID',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0      COMMENT '上级科室ID，0为顶级',
  `dept_name`   VARCHAR(100) NOT NULL                COMMENT '科室名称',
  `dept_code`   VARCHAR(50)  DEFAULT NULL            COMMENT '科室编码',
  `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`dept_id`),
  KEY `idx_dept_parent` (`parent_id`),
  KEY `idx_dept_code` (`dept_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室表';

-- ------------------------------------------------------------
-- 2. 用户表 sys_user（一人一档主体）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `user_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `emp_no`          VARCHAR(50)  NOT NULL                COMMENT '工号（唯一）',
  `username`        VARCHAR(50)  NOT NULL                COMMENT '登录账号（唯一）',
  `password`        VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt）',
  `real_name`       VARCHAR(50)  NOT NULL                COMMENT '姓名',
  `dept_id`         BIGINT       DEFAULT NULL            COMMENT '所属科室ID',
  `title`           VARCHAR(50)  DEFAULT NULL            COMMENT '职称',
  `phone`           VARCHAR(32)  DEFAULT NULL            COMMENT '手机号（加密存储，脱敏展示）',
  `email`           VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
  `avatar`          VARCHAR(255) DEFAULT NULL            COMMENT '头像地址',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0禁用',
  `del_flag`        TINYINT      NOT NULL DEFAULT 0      COMMENT '删除标记：0正常 1已删除',
  `last_login_time` DATETIME     DEFAULT NULL            COMMENT '最后登录时间',
  `create_by`       VARCHAR(50)  DEFAULT NULL            COMMENT '创建人',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`       VARCHAR(50)  DEFAULT NULL            COMMENT '更新人',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_empno` (`emp_no`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_dept` (`dept_id`),
  KEY `idx_user_name` (`real_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 3. 角色表 sys_role（五级角色 + 系统管理员）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role` (
  `role_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name`   VARCHAR(50)  NOT NULL                COMMENT '角色名称',
  `role_key`    VARCHAR(50)  NOT NULL                COMMENT '角色标识：doctor/dept_admin/auditor/committee/leader/admin',
  `data_scope`  TINYINT      NOT NULL DEFAULT 3      COMMENT '数据范围：1全部 2本科室 3本人',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL            COMMENT '备注',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ------------------------------------------------------------
-- 4. 用户角色关联表 sys_user_role（支持一人多角色）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ------------------------------------------------------------
-- 5. 菜单权限表 sys_menu（目录/菜单/按钮三级）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `menu_id`    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id`  BIGINT       NOT NULL DEFAULT 0      COMMENT '上级菜单ID，0为顶级',
  `menu_name`  VARCHAR(50)  NOT NULL                COMMENT '菜单名称',
  `menu_type`  CHAR(1)      NOT NULL DEFAULT 'C'    COMMENT '类型：M目录 C菜单 F按钮',
  `path`       VARCHAR(200) DEFAULT NULL            COMMENT '路由地址',
  `component`  VARCHAR(200) DEFAULT NULL            COMMENT '组件路径',
  `perms`      VARCHAR(100) DEFAULT NULL            COMMENT '权限标识，如 integrity:check:handle',
  `icon`       VARCHAR(50)  DEFAULT NULL            COMMENT '图标',
  `sort_order` INT          NOT NULL DEFAULT 0      COMMENT '排序',
  `status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1显示 0隐藏',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`menu_id`),
  KEY `idx_menu_parent` (`parent_id`),
  KEY `idx_menu_perms` (`perms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- ------------------------------------------------------------
-- 6. 角色菜单关联表 sys_role_menu
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`),
  KEY `idx_role_menu_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ------------------------------------------------------------
-- 7. 字典类型表 sys_dict_type
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
  `dict_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典类型ID',
  `dict_name` VARCHAR(100) NOT NULL                COMMENT '字典名称',
  `dict_type` VARCHAR(100) NOT NULL                COMMENT '字典类型编码（唯一）',
  `status`    TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
  `remark`    VARCHAR(255) DEFAULT NULL            COMMENT '备注',
  `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- ------------------------------------------------------------
-- 8. 字典数据表 sys_dict_data
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
  `dict_code`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
  `dict_type`   VARCHAR(100) NOT NULL                COMMENT '字典类型编码',
  `dict_label`  VARCHAR(100) NOT NULL                COMMENT '字典标签（中文名）',
  `dict_value`  VARCHAR(100) NOT NULL                COMMENT '字典键值',
  `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL            COMMENT '备注',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`dict_code`),
  UNIQUE KEY `uk_dict_data` (`dict_type`, `dict_value`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- ------------------------------------------------------------
-- 9. 操作日志表 sys_log（审计留痕，不可删除）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_log` (
  `log_id`     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`    BIGINT        DEFAULT NULL            COMMENT '操作人ID',
  `username`   VARCHAR(50)   DEFAULT NULL            COMMENT '操作人账号',
  `module`     VARCHAR(50)   DEFAULT NULL            COMMENT '模块（成果/评价/工单/申诉/系统）',
  `operation`  VARCHAR(100)  DEFAULT NULL            COMMENT '操作描述',
  `method`     VARCHAR(200)  DEFAULT NULL            COMMENT '请求方法',
  `params`     TEXT          DEFAULT NULL            COMMENT '请求参数',
  `ip`         VARCHAR(64)   DEFAULT NULL            COMMENT 'IP地址',
  `status`     TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1成功 0失败',
  `error_msg`  VARCHAR(2000) DEFAULT NULL            COMMENT '错误信息',
  `cost_time`  BIGINT        DEFAULT NULL            COMMENT '耗时(ms)',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_log_user` (`user_id`),
  KEY `idx_log_time` (`create_time`),
  KEY `idx_log_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ------------------------------------------------------------
-- 10. 登录日志表 sys_login_log
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `login_id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '登录日志ID',
  `username`   VARCHAR(50) NOT NULL                COMMENT '登录账号',
  `ip`         VARCHAR(64) DEFAULT NULL            COMMENT 'IP地址',
  `browser`    VARCHAR(100) DEFAULT NULL           COMMENT '浏览器',
  `os`         VARCHAR(100) DEFAULT NULL           COMMENT '操作系统',
  `status`     TINYINT     NOT NULL DEFAULT 1      COMMENT '状态：1成功 0失败',
  `msg`        VARCHAR(255) DEFAULT NULL           COMMENT '提示信息',
  `login_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`login_id`),
  KEY `idx_login_user` (`username`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ------------------------------------------------------------
-- 11. 科研成果主表 research_achievement
-- 成果类型 dict: ach_type（PAPER论文/TOPIC课题/PATENT专利软著/REWARD奖励/BOOK专著教材/STANDARD标准指南/POST学术任职/TRANSFER技术转化/OTHER其他）
-- 状态: 0草稿 1待科室初审 2待科研科终审 3已入库 4已退回 5已撤销 6已作废
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_achievement` (
  `ach_id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '成果ID',
  `user_id`        BIGINT        NOT NULL                COMMENT '申报人用户ID',
  `ach_type`       VARCHAR(20)   NOT NULL                COMMENT '成果类型（字典 ach_type）',
  `title`          VARCHAR(500)  NOT NULL                COMMENT '成果标题/名称',
  `ach_no`         VARCHAR(100)  DEFAULT NULL            COMMENT '编号：DOI/专利号/项目号/证书号',
  `source_name`    VARCHAR(200)  DEFAULT NULL            COMMENT '来源：期刊名/立项部门/颁奖单位',
  `publish_time`   DATETIME      DEFAULT NULL            COMMENT '发表/立项/授权时间',
  `level`          VARCHAR(50)   DEFAULT NULL            COMMENT '级别：期刊分区/项目级别/奖励级别（如 SCI-1区、国家级）',
  `rank_info`      VARCHAR(100)  DEFAULT NULL            COMMENT '位次/角色：如 第1作者、负责人',
  `is_corresponding` TINYINT     NOT NULL DEFAULT 0      COMMENT '是否通讯作者：1是 0否（仅论文）',
  `fund_amount`    DECIMAL(12,2) DEFAULT NULL            COMMENT '经费/到账金额（元）',
  `score`          DECIMAL(10,2) DEFAULT NULL            COMMENT '系统计算得分',
  `score_status`   TINYINT       NOT NULL DEFAULT 0      COMMENT '计分状态：0未计分 1已计分 2已作废回收',
  `status`         TINYINT       NOT NULL DEFAULT 0      COMMENT '审核状态：0草稿 1待科室初审 2待科研科终审 3已入库 4已退回 5已撤销 6已作废',
  `audit_by`       BIGINT        DEFAULT NULL            COMMENT '最近审核人ID',
  `audit_time`     DATETIME      DEFAULT NULL            COMMENT '最近审核时间',
  `audit_remark`   VARCHAR(500)  DEFAULT NULL            COMMENT '审核意见',
  `risk_flag`      TINYINT       NOT NULL DEFAULT 0      COMMENT '风险标记：0正常 1风险预警中 2已处置',
  `create_by`      BIGINT        DEFAULT NULL            COMMENT '创建人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`      BIGINT        DEFAULT NULL            COMMENT '更新人',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`ach_id`),
  UNIQUE KEY `uk_ach_user_no` (`user_id`, `ach_no`),
  KEY `idx_ach_type` (`ach_type`),
  KEY `idx_ach_status` (`status`),
  KEY `idx_ach_time` (`publish_time`),
  KEY `idx_ach_title` (`title`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科研成果主表';

-- ------------------------------------------------------------
-- 12. 附件表 research_attachment（成果/工单/申诉通用）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_attachment` (
  `file_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `biz_type`     VARCHAR(30)  NOT NULL                COMMENT '业务类型：ACH成果/CHECK工单/APPEAL申诉',
  `biz_id`       BIGINT       NOT NULL                COMMENT '业务ID',
  `file_name`    VARCHAR(255) NOT NULL                COMMENT '原始文件名',
  `file_path`    VARCHAR(500) NOT NULL                COMMENT '存储路径（UUID命名）',
  `file_size`    BIGINT       DEFAULT NULL            COMMENT '文件大小(字节)',
  `file_type`    VARCHAR(50)  DEFAULT NULL            COMMENT '文件扩展名',
  `md5`          VARCHAR(64)  DEFAULT NULL            COMMENT '文件MD5（去重）',
  `is_encrypted` TINYINT      NOT NULL DEFAULT 0      COMMENT '是否加密存储：1是 0否',
  `upload_by`    BIGINT       DEFAULT NULL            COMMENT '上传人',
  `upload_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`file_id`),
  KEY `idx_att_biz` (`biz_type`, `biz_id`),
  KEY `idx_att_md5` (`md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';

-- ------------------------------------------------------------
-- 13. 成果审核流水表 research_audit_log（多次退回重审留痕）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_audit_log` (
  `audit_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '审核流水ID',
  `ach_id`     BIGINT       NOT NULL                COMMENT '成果ID',
  `audit_type` VARCHAR(20)  NOT NULL                COMMENT '操作类型：SUBMIT提交 BACK退回 APPROVE终审通过 CANCEL撤销 INVALID作废',
  `audit_by`   BIGINT       DEFAULT NULL            COMMENT '操作人ID',
  `audit_name` VARCHAR(50)  DEFAULT NULL            COMMENT '操作人姓名',
  `opinion`    VARCHAR(500) DEFAULT NULL            COMMENT '审核意见',
  `audit_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`audit_id`),
  KEY `idx_audit_ach` (`ach_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成果审核流水表';

-- ------------------------------------------------------------
-- 14. 科研诚信年度评价表 research_integrity（汇总）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_integrity` (
  `integrity_id` BIGINT        NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `user_id`      BIGINT        NOT NULL                COMMENT '用户ID',
  `year`         INT           NOT NULL                COMMENT '评价年度',
  `period_type`  VARCHAR(20)   NOT NULL DEFAULT 'YEAR' COMMENT '周期类型：YEAR年度 PERIOD周期',
  `perf_score`   DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '业绩总分',
  `deduct_score` DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '有效诚信扣分',
  `total_score`  DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '最终得分',
  `level`        CHAR(1)       DEFAULT NULL            COMMENT '诚信等级：A/B/C/D',
  `veto_flag`    TINYINT       NOT NULL DEFAULT 0      COMMENT '是否一票否决：1是 0否',
  `rule_version` VARCHAR(20)   DEFAULT NULL            COMMENT '使用的规则版本',
  `calc_status`  TINYINT       NOT NULL DEFAULT 0      COMMENT '计算状态：0待计算 1已计算 2已重算',
  `remark`       VARCHAR(500)  DEFAULT NULL            COMMENT '评价说明',
  `assessor`     BIGINT        DEFAULT NULL            COMMENT '评定人ID',
  `assess_time`  DATETIME      DEFAULT NULL            COMMENT '评定时间',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`integrity_id`),
  UNIQUE KEY `uk_integrity_user_year` (`user_id`, `year`, `period_type`),
  KEY `idx_integrity_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科研诚信年度评价表';

-- ------------------------------------------------------------
-- 15. 评价明细快照表 research_integrity_detail（可追溯"分数构成"）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_integrity_detail` (
  `detail_id`    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `integrity_id` BIGINT        NOT NULL                COMMENT '评价ID',
  `user_id`      BIGINT        NOT NULL                COMMENT '用户ID',
  `year`         INT           NOT NULL                COMMENT '评价年度',
  `biz_type`     VARCHAR(20)   NOT NULL                COMMENT '类型：PERF业绩 DEDUCT扣分',
  `ach_id`       BIGINT        DEFAULT NULL            COMMENT '关联成果ID（业绩明细）',
  `violation_id` BIGINT        DEFAULT NULL            COMMENT '关联违规记录ID（扣分明细）',
  `rule_id`      BIGINT        DEFAULT NULL            COMMENT '规则ID',
  `rule_version` VARCHAR(20)   DEFAULT NULL            COMMENT '规则版本',
  `item_name`    VARCHAR(200)  DEFAULT NULL            COMMENT '计分项名称，如 SCI一区·第一作者',
  `base_score`   DECIMAL(10,2) DEFAULT NULL            COMMENT '基础分',
  `coefficient`  DECIMAL(5,2)  DEFAULT NULL            COMMENT '系数',
  `score`        DECIMAL(10,2) DEFAULT NULL            COMMENT '得分/扣分（扣分为负数）',
  `snapshot`     JSON          DEFAULT NULL            COMMENT '规则快照（当时分值配置）',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`detail_id`),
  KEY `idx_detail_integrity` (`integrity_id`),
  KEY `idx_detail_ach` (`ach_id`),
  KEY `idx_detail_user_year` (`user_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价明细快照表';

-- ------------------------------------------------------------
-- 16. 评分规则表 research_rule
-- rule_type: PERF业绩 DEDUCT扣分；calc_mode: FIXED固定 COEFF系数 ROLE_MAP角色映射 MONEY金额
-- 版本化：修改规则发布新版本，不修改旧版本
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_rule` (
  `rule_id`       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_type`     VARCHAR(20)   NOT NULL                COMMENT '规则类型：PERF业绩 DEDUCT扣分',
  `rule_no`       VARCHAR(30)   NOT NULL                COMMENT '规则编号（唯一），如 PERF-PAPER-01',
  `ach_type`      VARCHAR(20)   DEFAULT NULL            COMMENT '成果类型/违规类型（字典）',
  `rule_name`     VARCHAR(200)  NOT NULL                COMMENT '规则名称',
  `base_score`    DECIMAL(10,2) DEFAULT NULL            COMMENT '基础分',
  `calc_mode`     VARCHAR(20)   NOT NULL DEFAULT 'FIXED' COMMENT '计算模式：FIXED/COEFF/ROLE_MAP/MONEY',
  `config_json`   JSON          DEFAULT NULL            COMMENT '配置：ROLE_MAP分值映射 或 MONEY{unit_amount,score_per_unit,cap}',
  `version`       VARCHAR(20)   NOT NULL DEFAULT '1.0'  COMMENT '规则版本',
  `effective_date` DATE         NOT NULL                COMMENT '生效日期',
  `expire_date`   DATE          DEFAULT NULL            COMMENT '失效日期（NULL为长期有效）',
  `status`        TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
  `veto_flag`     TINYINT       NOT NULL DEFAULT 0      COMMENT '是否一票否决：1是 0否（扣分规则）',
  `need_reform`   TINYINT       NOT NULL DEFAULT 0      COMMENT '是否需整改：1是 0否（B/C级）',
  `remark`        VARCHAR(255)  DEFAULT NULL            COMMENT '备注',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_rule_no` (`rule_no`),
  KEY `idx_rule_type` (`rule_type`, `ach_type`),
  KEY `idx_rule_version` (`version`, `effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分规则表';

-- ------------------------------------------------------------
-- 17. 系数表 research_rule_coeff（作者位次/角色/排名系数；rule_id 为空=全局通用）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_rule_coeff` (
  `coeff_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '系数ID',
  `rule_id`      BIGINT       DEFAULT NULL            COMMENT '规则ID（NULL=该系数类型通用）',
  `coeff_type`   VARCHAR(30)  NOT NULL                COMMENT '系数类型：RANK作者位次 TOPIC_ROLE课题角色 REWARD_RANK获奖排名',
  `position_key` VARCHAR(30)  NOT NULL                COMMENT '档位键：1ST/CORRESP/2ND/3RD/4TH/5TH+ 或 LEADER/CORE/MEMBER',
  `position_label` VARCHAR(50) DEFAULT NULL           COMMENT '档位说明，如 第一作者/通讯作者',
  `coefficient`  DECIMAL(5,2) NOT NULL                COMMENT '系数值',
  `sort_order`   INT          NOT NULL DEFAULT 0      COMMENT '排序',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`coeff_id`),
  UNIQUE KEY `uk_coeff` (`coeff_type`, `position_key`, `rule_id`),
  KEY `idx_coeff_rule` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分系数表';

-- ------------------------------------------------------------
-- 18. 诚信等级配置表 research_level_config
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_level_config` (
  `level_id`       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `level`          CHAR(1)       NOT NULL                COMMENT '等级：A/B/C/D',
  `level_name`     VARCHAR(20)   NOT NULL                COMMENT '等级名称',
  `min_deduct`     DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '有效扣分下限（含）',
  `max_deduct`     DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '有效扣分上限（含）',
  `veto_flag`      TINYINT       NOT NULL DEFAULT 0      COMMENT '是否一票否决直接判定：1是 0否',
  `conditions`     VARCHAR(500)  DEFAULT NULL            COMMENT '附加判定条件说明',
  `version`        VARCHAR(20)   NOT NULL DEFAULT '1.0'  COMMENT '版本',
  `effective_date` DATE          NOT NULL                COMMENT '生效日期',
  `status`         TINYINT       NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`level_id`),
  UNIQUE KEY `uk_level` (`level`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诚信等级配置表';

-- ------------------------------------------------------------
-- 19. 违规记录表 research_violation
-- 状态: CONFIRMED已认定 EFFECTIVE已生效 REFORMING整改中 REFORMED整改完成 REVOKED已撤销
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_violation` (
  `violation_id`   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '违规记录ID',
  `user_id`        BIGINT        NOT NULL                COMMENT '违规人用户ID',
  `ach_id`         BIGINT        DEFAULT NULL            COMMENT '关联成果ID',
  `check_id`       BIGINT        DEFAULT NULL            COMMENT '来源核查工单ID',
  `violation_type` VARCHAR(30)   NOT NULL                COMMENT '违规类型（字典 violation_type）',
  `violation_level` CHAR(1)      NOT NULL                COMMENT '违规等级：B/C/D',
  `deduct_score`   DECIMAL(10,2) NOT NULL DEFAULT 0      COMMENT '认定扣分（一票否决记0，以veto_flag标识）',
  `description`    VARCHAR(1000) DEFAULT NULL            COMMENT '违规描述',
  `evidence`       VARCHAR(1000) DEFAULT NULL            COMMENT '证据说明',
  `status`         VARCHAR(20)   NOT NULL DEFAULT 'CONFIRMED' COMMENT '状态：CONFIRMED/EFFECTIVE/REFORMING/REFORMED/REVOKED',
  `effective_date` DATE          DEFAULT NULL            COMMENT '扣分生效日期',
  `veto_flag`      TINYINT       NOT NULL DEFAULT 0      COMMENT '是否一票否决：1是 0否',
  `reform_deadline` DATE         DEFAULT NULL            COMMENT '整改期限',
  `reform_result`  VARCHAR(1000) DEFAULT NULL            COMMENT '整改情况说明',
  `reform_check_by` BIGINT       DEFAULT NULL            COMMENT '整改验收人ID',
  `reform_check_time` DATETIME   DEFAULT NULL            COMMENT '整改验收时间',
  `create_by`      BIGINT        DEFAULT NULL            COMMENT '创建人',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`violation_id`),
  KEY `idx_violation_user` (`user_id`),
  KEY `idx_violation_status` (`status`),
  KEY `idx_violation_type` (`violation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规记录表';

-- ------------------------------------------------------------
-- 20. 失信核查工单表 research_check
-- 状态: PENDING待认领 PROCESSING核查中 TO_CONFIRM待认定 CONFIRMED已认定
--       TO_PUBLIC待公示 PUBLISHED已生效 ARCHIVED已归档 DISMISSED误报撤销
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_check` (
  `check_id`      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `check_no`      VARCHAR(30)   NOT NULL                COMMENT '工单号，如 CK20260101001',
  `user_id`       BIGINT        NOT NULL                COMMENT '被核查人用户ID',
  `ach_id`        BIGINT        DEFAULT NULL            COMMENT '关联成果ID',
  `risk_source`   VARCHAR(20)   NOT NULL DEFAULT 'AUTO' COMMENT '来源：AUTO自动筛查 MANUAL人工',
  `risk_type`     VARCHAR(50)   DEFAULT NULL            COMMENT '风险类型（字典 risk_type）',
  `risk_desc`     VARCHAR(1000) DEFAULT NULL            COMMENT '风险描述',
  `risk_log_id`   BIGINT        DEFAULT NULL            COMMENT '来源预警记录ID',
  `status`        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/TO_CONFIRM/CONFIRMED/TO_PUBLIC/PUBLISHED/ARCHIVED/DISMISSED',
  `assignee`      BIGINT        DEFAULT NULL            COMMENT '认领人（科研科审核员）',
  `claim_time`    DATETIME      DEFAULT NULL            COMMENT '认领时间',
  `conclusion`    VARCHAR(1000) DEFAULT NULL            COMMENT '调查结论',
  `deduct_score`  DECIMAL(10,2) DEFAULT NULL            COMMENT '认定扣分',
  `violation_id`  BIGINT        DEFAULT NULL            COMMENT '认定后生成的违规记录ID',
  `handle_result` VARCHAR(500)  DEFAULT NULL            COMMENT '处置结果说明',
  `publicity_id`  BIGINT        DEFAULT NULL            COMMENT '关联公示记录ID',
  `finish_time`   DATETIME      DEFAULT NULL            COMMENT '办结时间',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`check_id`),
  UNIQUE KEY `uk_check_no` (`check_no`),
  KEY `idx_check_user` (`user_id`),
  KEY `idx_check_status` (`status`),
  KEY `idx_check_assignee` (`assignee`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失信核查工单表';

-- ------------------------------------------------------------
-- 21. 核查记录表 research_check_record（取证/调查/认定/处置留痕）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_check_record` (
  `record_id`   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `check_id`    BIGINT        NOT NULL                COMMENT '工单ID',
  `record_type` VARCHAR(20)   NOT NULL                COMMENT '类型：EVIDENCE取证 INVESTIGATE调查 CONFIRM认定 HANDLE处置 SIGN会签',
  `content`     TEXT          DEFAULT NULL            COMMENT '记录内容',
  `operator`    BIGINT        DEFAULT NULL            COMMENT '操作人ID',
  `operator_name` VARCHAR(50) DEFAULT NULL            COMMENT '操作人姓名',
  `operate_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_record_check` (`check_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核查记录表';

-- ------------------------------------------------------------
-- 22. 申诉表 research_appeal
-- 状态: PENDING待受理 REVIEWING复核中 SUSTAINED维持原判 OVERTURNED变更裁定 REJECTED驳回
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_appeal` (
  `appeal_id`   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '申诉ID',
  `appeal_no`   VARCHAR(30)   NOT NULL                COMMENT '申诉编号，如 AP20260101001',
  `user_id`     BIGINT        NOT NULL                COMMENT '申诉人用户ID',
  `appeal_type` VARCHAR(20)   NOT NULL                COMMENT '申诉对象：SCORE评价结果 DEDUCT扣分 CONFIRM认定结论',
  `biz_type`    VARCHAR(20)   NOT NULL                COMMENT '目标类型：INTEGRITY/VIOLATION/CHECK',
  `biz_id`      BIGINT        NOT NULL                COMMENT '目标ID',
  `reason`      VARCHAR(1000) NOT NULL                COMMENT '申诉理由',
  `status`      VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/REVIEWING/SUSTAINED/OVERTURNED/REJECTED',
  `result`      VARCHAR(1000) DEFAULT NULL            COMMENT '复核裁定结果',
  `result_by`   BIGINT        DEFAULT NULL            COMMENT '裁定人（诚信委员会）',
  `result_time` DATETIME      DEFAULT NULL            COMMENT '裁定时间',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`appeal_id`),
  UNIQUE KEY `uk_appeal_no` (`appeal_no`),
  KEY `idx_appeal_user` (`user_id`),
  KEY `idx_appeal_biz` (`biz_type`, `biz_id`),
  KEY `idx_appeal_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申诉表';

-- ------------------------------------------------------------
-- 23. 黑名单表 research_blacklist（风险期刊/出版社/关键词）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_blacklist` (
  `bl_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '黑名单ID',
  `bl_type`     VARCHAR(20)  NOT NULL                COMMENT '类型：JOURNAL期刊 PUBLISHER出版社 KEYWORD关键词',
  `bl_name`     VARCHAR(255) NOT NULL                COMMENT '名称/关键词',
  `source`      VARCHAR(100) DEFAULT NULL            COMMENT '来源：中科院预警名单2025/自定义',
  `risk_level`  VARCHAR(20)  DEFAULT NULL            COMMENT '风险等级：高/中/低',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
  `remark`      VARCHAR(255) DEFAULT NULL            COMMENT '备注',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`bl_id`),
  KEY `idx_bl_type` (`bl_type`),
  KEY `idx_bl_name` (`bl_name`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单表';

-- ------------------------------------------------------------
-- 24. 风险筛查记录表 research_risk_log（自动风控命中）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_risk_log` (
  `risk_id`     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '预警记录ID',
  `user_id`     BIGINT        NOT NULL                COMMENT '涉事用户ID',
  `ach_id`      BIGINT        DEFAULT NULL            COMMENT '关联成果ID',
  `risk_type`   VARCHAR(50)   NOT NULL                COMMENT '风险类型（字典 risk_type）',
  `rule_no`     VARCHAR(50)   DEFAULT NULL            COMMENT '命中筛查规则',
  `risk_desc`   VARCHAR(1000) DEFAULT NULL            COMMENT '风险描述',
  `match_value` VARCHAR(500)  DEFAULT NULL            COMMENT '命中依据：DOI/标题/时间等',
  `status`      VARCHAR(20)   NOT NULL DEFAULT 'NEW'  COMMENT '状态：NEW新增 CLAIMED已认领 DISMISSED误报 CONFIRMED确认',
  `check_id`    BIGINT        DEFAULT NULL            COMMENT '转成的工单ID',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`risk_id`),
  KEY `idx_risk_user` (`user_id`),
  KEY `idx_risk_status` (`status`),
  KEY `idx_risk_type` (`risk_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险筛查记录表';

-- ------------------------------------------------------------
-- 25. 通知消息表 research_notice
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_notice` (
  `notice_id`   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id`     BIGINT       NOT NULL                COMMENT '接收人用户ID',
  `notice_type` VARCHAR(20)  NOT NULL                COMMENT '类型：RISK预警 AUDIT审核 APPEAL申诉 EVALUATE评价 SYSTEM系统',
  `title`       VARCHAR(200) NOT NULL                COMMENT '标题',
  `content`     TEXT         DEFAULT NULL            COMMENT '内容',
  `biz_type`    VARCHAR(20)  DEFAULT NULL            COMMENT '关联业务类型',
  `biz_id`      BIGINT       DEFAULT NULL            COMMENT '关联业务ID',
  `is_read`     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读：0未读 1已读',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`notice_id`),
  KEY `idx_notice_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知消息表';

-- ------------------------------------------------------------
-- 26. 公示记录表 research_publicity
-- 状态: PUBLISHING公示中 FINISHED已结束 CANCELED已撤销
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `research_publicity` (
  `publicity_id`  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公示ID',
  `publicity_no`  VARCHAR(30)  NOT NULL                COMMENT '公示编号',
  `publicity_type` VARCHAR(20) NOT NULL                COMMENT '类型：EVALUATE评价结果公示 CONFIRM失信认定公示',
  `biz_type`      VARCHAR(20)  NOT NULL                COMMENT '关联对象类型：INTEGRITY/CHECK',
  `biz_ids`       JSON         DEFAULT NULL            COMMENT '关联对象ID列表',
  `scope`         VARCHAR(20)  NOT NULL DEFAULT 'ALL'  COMMENT '公示范围：ALL全院 DEPT科室 OWN个人',
  `start_time`    DATETIME     NOT NULL                COMMENT '公示开始时间',
  `end_time`      DATETIME     NOT NULL                COMMENT '公示结束时间',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHING' COMMENT '状态：PUBLISHING/FINISHED/CANCELED',
  `create_by`     BIGINT       DEFAULT NULL            COMMENT '创建人',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`publicity_id`),
  UNIQUE KEY `uk_publicity_no` (`publicity_no`),
  KEY `idx_publicity_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公示记录表';
