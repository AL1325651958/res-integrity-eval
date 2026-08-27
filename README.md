# 医院个人科研成果诚信综合评价系统

面向医院内网的"一人一档"科研诚信评价系统：成果自动归集入库、业绩加分 + 诚信扣分双维度量化评分、A/B/C/D 诚信等级自动判定、智能风险预警、工单核查处置、申诉复核、三级数据看板，评价结果对接职称评审、导师遴选、项目申报等院内业务。

> 本仓库当前包含 **设计与交付包** + **前后端完整功能源码**（软著申请所需的源程序即来自 `backend/` 与 `frontend/`）。

## 一、文档导航

| 文档 | 内容 | 读者 |
|---|---|---|
| `docs/部署运维手册.md` | 服务器环境要求、部署步骤、Nginx/systemd 配置、备份 | 运维 |
| `scripts/deploy.sh` | 一键部署脚本（数据库+后端+前端+Nginx） | 运维 |
| `docs/接口约定.md` | 前后端 API 契约（路径/响应/前端函数名/菜单组件映射） | 开发 |
| `docs/技术架构设计.md` | 技术栈决策、前后端设计、权限、部署、非功能指标 | 开发 / 运维 |
| `docs/业绩评分规则.md` | 业绩加分分值表、诚信扣分表、等级判定阈值（规则 V1.0） | 科研科 / 诚信委员会 / 开发 |
| `docs/数据库设计.md` | 26 张表设计说明、E-R 总览、状态机、索引约束 | 开发 |
| `docs/软件说明书.md` | 用户操作手册（软著文档鉴别材料底稿） | 全体用户 / 软著申请 |
| `docs/软件著作权申请交付清单.md` | 软著申请材料清单、源程序/文档排版规范、申请流程 | 项目负责人 / 法务 |
| `sql/schema.sql` | 完整建库建表 DDL（MySQL 8.0） | 开发 / DBA |
| `sql/init_dict.sql` | 字典数据（成果类型、状态、违规类型等） | 开发 |
| `sql/init_rules.sql` | 评分规则、系数、等级阈值初始数据 | 开发 |

## 二、目录结构

```
医院个人科研成果诚信综合评价系统/
├─ README.md
├─ docs/                       # 设计文档 + 接口契约 + 软著材料
├─ sql/                        # 建库/字典/规则初始化
├─ backend/                    # Spring Boot 3 后端（Maven 工程，126 个 Java 文件）
│  ├─ pom.xml
│  └─ src/main/java/com/hospital/integrity/
│     ├─ controller/           # 18 个 REST 控制器（auth/achievement/integrity/risk/appeal/dashboard/system...）
│     ├─ service/              # 21 个服务（计分引擎、风控筛查、工单流转、年度评价、看板统计...）
│     ├─ entity/ + mapper/     # 26 表实体 + MyBatis-Plus Mapper
│     ├─ security/             # JWT 认证、五级角色权限
│     ├─ config/               # MyBatis-Plus、操作日志切面、启动初始化（种子数据）
│     ├─ task/                 # 定时任务（年度评价/风控筛查/整改提醒）
│     └─ dto/ common/ util/
└─ frontend/                   # Vue3 + TS 前端（45+ 文件）
   ├─ package.json
   └─ src/
      ├─ api/                  # 10 个 API 模块（与接口契约一致）
      ├─ views/                # 25 个页面（登录/看板/成果/诚信/风险/申诉/系统管理）
      ├─ router/ stores/ layout/ utils/ composables/ directives/
```

## 三、快速开始

### 3.1 数据库初始化

```bash
mysql -u root -p < sql/schema.sql        # 建库建表（integrity_db，utf8mb4）
mysql -u root -p integrity_db < sql/init_dict.sql
mysql -u root -p integrity_db < sql/init_rules.sql
```

### 3.2 后端（Spring Boot 3 / JDK 17）

```bash
cd backend
# 配置数据库连接：src/main/resources/application.yml（或环境变量 DB_PASSWORD）
mvn clean package
java -jar target/integrity-system.jar
# 启动后自动初始化：admin 账号（密码 admin123）、6 类角色、菜单与角色-菜单
```

### 3.3 前端（Vue3 + Vite）

```bash
cd frontend
npm install
npm run dev        # 开发模式，/api 代理到 http://localhost:8080
npm run build      # 生产构建（输出 dist/，由 Nginx 托管并反代 /api）
```

### 3.4 一键部署（Linux 服务器）

```bash
# 全量部署（需 root；自动：建库→导入→构建后端→注册 systemd→构建前端→配置 Nginx）
DB_PASSWORD=你的数据库密码 sudo bash scripts/deploy.sh

# 分步执行
sudo bash scripts/deploy.sh --db-only          # 仅初始化数据库
sudo bash scripts/deploy.sh --backend-only     # 仅构建并启动后端
sudo bash scripts/deploy.sh --frontend-only    # 仅构建前端并配置 Nginx
```

### 3.5 初始账号

| 账号 | 角色 | 说明 |
|---|---|---|
| `admin / admin123` | 系统管理员 | 全部功能；首次登录后请修改密码 |
| 其余角色 | doctor/dept_admin/auditor/committee/leader | 由管理员在"系统管理 → 用户管理"中创建并分配角色 |

## 四、核心功能实现状态

| 模块 | 实现内容 |
|---|---|
| 认证与权限 | JWT 无状态认证、五级角色 + 管理员、按钮级 perms、动态菜单路由、登录/操作日志 |
| 成果管理 | 8 类成果动态表单填报、附件上传下载、科室初审/科研科终审两级审核、入库自动计分、作废回收计分、Excel 批量导入/导出 |
| 诚信评价 | 年度评价自动计算（业绩分 + 有效扣分 + A/B/C/D 判定）、明细快照可追溯、档案 PDF 导出、评价结果公示、手动触发重算 |
| 风控处置 | 成果入库实时筛查 + 每日定时筛查（重复申报/一稿多投/黑名单期刊/时间逻辑/署名异常）、预警转工单、取证调查、失信认定、公示生效、误报撤销、整改与验收（B/C 级按规则减免）、申诉复核 |
| 看板 | 个人/科室/全院三级统计（ECharts：折线/柱状/饼图） |
| 系统管理 | 用户/科室/角色/菜单/字典/评分规则/系数/等级阈值/黑名单/日志审计/通知 |

## 五、路线图

1. ✅ 需求基线（原始设计文档）
2. ✅ 技术架构 / 评分规则 / 数据库设计 / 软著交付包（设计阶段）
3. ✅ 后端完整功能源码（126 个 Java 文件）
4. ✅ 前端完整功能源码（45+ 个 TS/Vue 文件，`vue-tsc` 类型检查零错误）
5. ⏳ 内网部署联调（需 JDK17 + MySQL 环境；本机无 Java 无法编译验证后端，前端打包受本机沙箱限制未产出 dist，代码已通过类型检查）
6. ⏳ 软著申请材料定稿（源程序整理规范见 `docs/软件著作权申请交付清单.md` 第 3 节）
