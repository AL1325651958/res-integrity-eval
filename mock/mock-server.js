/**
 * 医院个人科研成果诚信综合评价系统 —— 本地演示服务（Mock）
 *
 * 用途：本机无 JDK/MySQL 时，用前端生产构建产物（frontend/dist）+
 *      内存模拟数据，按 docs/接口约定.md 的 API 契约提供接口，
 *      让界面可完整点击演示（登录/看板/成果/诚信/风控/申诉/系统管理）。
 *
 * 启动：node mock-server.js  （默认端口 8080，浏览器访问 http://127.0.0.1:8080）
 * 依赖：express（npm i express@4 --ignore-scripts）
 *
 * 演示账号（密码均 123456，管理员为 admin123）：
 *   admin     系统管理员     doctor    普通医护人员
 *   dept      科室管理员     auditor   科研科审核员
 *   committee 科研诚信委员会  leader    院领导
 */
const express = require('express')
const path = require('path')

const PORT = process.env.PORT || 8080
const DIST = path.join(__dirname, '..', 'frontend', 'dist')

const app = express()
app.use(express.json())

// ============================================================
// 内存演示数据
// ============================================================
let idSeq = 1000
const nextId = () => ++idSeq

const USERS = [
  { userId: 1, empNo: '0000', username: 'admin', realName: '系统管理员', deptId: 1, title: '高级工程师', roles: ['admin'] },
  { userId: 2, empNo: '1001', username: 'doctor', realName: '张伟', deptId: 2, title: '主任医师', roles: ['doctor'] },
  { userId: 3, empNo: '1002', username: 'dept', realName: '李娜', deptId: 2, title: '护士长', roles: ['dept_admin'] },
  { userId: 4, empNo: '2001', username: 'auditor', realName: '王强', deptId: 1, title: '科研科科员', roles: ['auditor'] },
  { userId: 5, empNo: '2002', username: 'committee', realName: '赵敏', deptId: 1, title: '科研诚信委员会', roles: ['committee'] },
  { userId: 6, empNo: '3001', username: 'leader', realName: '刘建国', deptId: 1, title: '院长', roles: ['leader'] }
]
const PWD = { admin: 'admin123', doctor: '123456', dept: '123456', auditor: '123456', committee: '123456', leader: '123456' }

const DEPTS = [
  { deptId: 1, parentId: 0, deptName: '医院', deptCode: 'ROOT', sortOrder: 1, status: 1, children: [] },
  { deptId: 2, parentId: 1, deptName: '骨科', deptCode: 'GK', sortOrder: 1, status: 1, children: [] },
  { deptId: 3, parentId: 1, deptName: '心内科', deptCode: 'XNK', sortOrder: 2, status: 1, children: [] },
  { deptId: 4, parentId: 1, deptName: '检验科', deptCode: 'JYK', sortOrder: 3, status: 1, children: [] }
]

const ROLES = [
  { roleId: 1, roleName: '系统管理员', roleKey: 'admin', dataScope: 1, status: 1 },
  { roleId: 2, roleName: '普通医护人员', roleKey: 'doctor', dataScope: 3, status: 1 },
  { roleId: 3, roleName: '科室管理员', roleKey: 'dept_admin', dataScope: 2, status: 1 },
  { roleId: 4, roleName: '科研科审核员', roleKey: 'auditor', dataScope: 1, status: 1 },
  { roleId: 5, roleName: '科研诚信委员会', roleKey: 'committee', dataScope: 1, status: 1 },
  { roleId: 6, roleName: '院领导', roleKey: 'leader', dataScope: 1, status: 1 }
]

// 菜单树（与后端 SysInitRunner 种子一致）
const MENUS = [
  { menuId: 1, parentId: 0, menuName: '工作台', menuType: 'M', path: '/dashboard', component: null, icon: 'Odometer', sortOrder: 1, children: [
    { menuId: 11, parentId: 1, menuName: '个人工作台', menuType: 'C', path: '/dashboard/my', component: 'dashboard/MyDashboard', icon: null, sortOrder: 1, children: [] },
    { menuId: 12, parentId: 1, menuName: '科室看板', menuType: 'C', path: '/dashboard/dept', component: 'dashboard/DeptDashboard', icon: null, sortOrder: 2, children: [] },
    { menuId: 13, parentId: 1, menuName: '全院看板', menuType: 'C', path: '/dashboard/hospital', component: 'dashboard/HospitalDashboard', icon: null, sortOrder: 3, children: [] }
  ] },
  { menuId: 2, parentId: 0, menuName: '成果管理', menuType: 'M', path: '/achievement', component: null, icon: 'Document', sortOrder: 2, children: [
    { menuId: 21, parentId: 2, menuName: '我的成果', menuType: 'C', path: '/achievement/my', component: 'achievement/MyAchievements', icon: null, sortOrder: 1, children: [] },
    { menuId: 22, parentId: 2, menuName: '成果审核', menuType: 'C', path: '/achievement/audit', component: 'achievement/AuditAchievements', icon: null, sortOrder: 2, children: [] }
  ] },
  { menuId: 3, parentId: 0, menuName: '诚信评价', menuType: 'M', path: '/integrity', component: null, icon: 'Medal', sortOrder: 3, children: [
    { menuId: 31, parentId: 3, menuName: '我的诚信档案', menuType: 'C', path: '/integrity/my', component: 'integrity/MyIntegrity', icon: null, sortOrder: 1, children: [] },
    { menuId: 32, parentId: 3, menuName: '评价管理', menuType: 'C', path: '/integrity/admin', component: 'integrity/IntegrityAdmin', icon: null, sortOrder: 2, children: [] }
  ] },
  { menuId: 4, parentId: 0, menuName: '风险与处置', menuType: 'M', path: '/risk', component: null, icon: 'Warning', sortOrder: 4, children: [
    { menuId: 41, parentId: 4, menuName: '风险预警', menuType: 'C', path: '/risk/log', component: 'risk/RiskLog', icon: null, sortOrder: 1, children: [] },
    { menuId: 42, parentId: 4, menuName: '核查工单', menuType: 'C', path: '/risk/check', component: 'risk/CheckList', icon: null, sortOrder: 2, children: [] },
    { menuId: 43, parentId: 4, menuName: '违规与整改', menuType: 'C', path: '/risk/violation', component: 'risk/ViolationList', icon: null, sortOrder: 3, children: [] }
  ] },
  { menuId: 5, parentId: 0, menuName: '申诉中心', menuType: 'M', path: '/appeal', component: null, icon: 'ChatLineRound', sortOrder: 5, children: [
    { menuId: 51, parentId: 5, menuName: '我的申诉', menuType: 'C', path: '/appeal/list', component: 'appeal/AppealList', icon: null, sortOrder: 1, children: [] }
  ] },
  { menuId: 6, parentId: 0, menuName: '系统管理', menuType: 'M', path: '/system', component: null, icon: 'Setting', sortOrder: 6, children: [
    { menuId: 61, parentId: 6, menuName: '用户管理', menuType: 'C', path: '/system/user', component: 'system/UserManage', icon: null, sortOrder: 1, children: [] },
    { menuId: 62, parentId: 6, menuName: '科室管理', menuType: 'C', path: '/system/dept', component: 'system/DeptManage', icon: null, sortOrder: 2, children: [] },
    { menuId: 63, parentId: 6, menuName: '角色权限', menuType: 'C', path: '/system/role', component: 'system/RoleManage', icon: null, sortOrder: 3, children: [] },
    { menuId: 64, parentId: 6, menuName: '菜单管理', menuType: 'C', path: '/system/menu', component: 'system/MenuManage', icon: null, sortOrder: 4, children: [] },
    { menuId: 65, parentId: 6, menuName: '字典管理', menuType: 'C', path: '/system/dict', component: 'system/DictManage', icon: null, sortOrder: 5, children: [] },
    { menuId: 66, parentId: 6, menuName: '规则配置', menuType: 'C', path: '/system/rule', component: 'system/RuleManage', icon: null, sortOrder: 6, children: [] },
    { menuId: 67, parentId: 6, menuName: '黑名单管理', menuType: 'C', path: '/system/blacklist', component: 'system/BlacklistManage', icon: null, sortOrder: 7, children: [] },
    { menuId: 68, parentId: 6, menuName: '日志审计', menuType: 'C', path: '/system/log', component: 'system/LogManage', icon: null, sortOrder: 8, children: [] }
  ] },
  { menuId: 7, parentId: 0, menuName: '通知中心', menuType: 'C', path: '/notice', component: 'notice/index', icon: 'Bell', sortOrder: 7, children: [] },
  { menuId: 8, parentId: 0, menuName: '个人中心', menuType: 'C', path: '/profile', component: 'profile/index', icon: 'User', sortOrder: 8, children: [] }
]

// 角色-菜单（与后端 SysInitRunner 一致）
const ROLE_MENUS = {
  admin: [1, 11, 12, 13, 2, 21, 22, 3, 31, 32, 4, 41, 42, 43, 5, 51, 6, 61, 62, 63, 64, 65, 66, 67, 68, 7, 8],
  doctor: [1, 11, 2, 21, 3, 31, 5, 51, 7, 8],
  dept_admin: [1, 11, 12, 2, 21, 22, 3, 31, 5, 51, 7, 8],
  auditor: [1, 11, 12, 2, 21, 22, 3, 31, 32, 4, 41, 42, 43, 5, 51, 7, 8],
  committee: [1, 11, 2, 21, 3, 31, 32, 4, 41, 42, 43, 5, 51, 6, 66, 67, 7, 8],
  leader: [1, 11, 12, 13, 3, 31, 32, 4, 43, 7, 8]
}

const now = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

let ACH = [
  { achId: 1, userId: 2, achType: 'PAPER', title: '关节置换术后快速康复的临床研究', achNo: '10.1000/j.2026.01.001', sourceName: '中华骨科杂志', publishTime: '2026-03-15 10:00:00', level: 'EI/北大核心', rankInfo: '第1作者', isCorresponding: 1, fundAmount: null, score: 15, scoreStatus: 1, status: 3, auditRemark: null, createTime: '2026-03-20 09:12:00' },
  { achId: 2, userId: 2, achType: 'PAPER', title: 'MicroRNA-21 在骨肉瘤中的表达及意义', achNo: '10.1000/j.2026.02.008', sourceName: 'Cell Research', publishTime: '2026-02-10 00:00:00', level: 'SCI-1区', rankInfo: '通讯作者', isCorresponding: 1, fundAmount: null, score: 50, scoreStatus: 1, status: 3, auditRemark: null, createTime: '2026-02-15 14:30:00' },
  { achId: 3, userId: 2, achType: 'TOPIC', title: '3D 打印人工椎体临床应用研究', achNo: 'NSFC-82070001', sourceName: '国家自然科学基金委', publishTime: '2025-06-01 00:00:00', level: '国家级', rankInfo: '负责人', isCorresponding: 0, fundAmount: 800000, score: 48, scoreStatus: 1, status: 3, auditRemark: null, createTime: '2025-06-10 10:00:00' },
  { achId: 4, userId: 2, achType: 'PATENT', title: '一种可调式脊柱内固定装置', achNo: 'ZL2023101234567', sourceName: '国家知识产权局', publishTime: '2026-01-08 00:00:00', level: '发明专利', rankInfo: '第1发明人', isCorresponding: 0, fundAmount: null, score: 30, scoreStatus: 1, status: 3, auditRemark: null, createTime: '2026-01-12 16:40:00' },
  { achId: 5, userId: 3, achType: 'PAPER', title: '骨科护理路径对术后并发症的影响', achNo: '10.2000/n.2026.03.002', sourceName: '护理研究', publishTime: '2026-04-01 00:00:00', level: '科技核心', rankInfo: '第1作者', isCorresponding: 0, fundAmount: null, score: null, scoreStatus: 0, status: 1, auditRemark: null, createTime: '2026-08-20 09:00:00' },
  { achId: 6, userId: 3, achType: 'REWARD', title: '骨科围手术期管理创新实践', achNo: 'YJ2025-3-021', sourceName: '省医学会', publishTime: '2025-12-20 00:00:00', level: '省部级二等奖', rankInfo: '第2名', isCorresponding: 0, fundAmount: null, score: 24, scoreStatus: 1, status: 3, auditRemark: null, createTime: '2025-12-25 11:20:00' },
  { achId: 7, userId: 4, achType: 'TOPIC', title: '医院科研诚信智能风控体系研究', achNo: 'KY2026-01', sourceName: '市卫健委', publishTime: '2026-07-01 00:00:00', level: '市厅级', rankInfo: '负责人', isCorresponding: 0, fundAmount: 200000, score: null, scoreStatus: 0, status: 2, auditRemark: null, createTime: '2026-07-05 10:30:00' },
  { achId: 8, userId: 4, achType: 'PAPER', title: '科研诚信评价指标体系构建初探', achNo: '10.3000/r.2026.05.011', sourceName: '中华医学科研管理杂志', publishTime: '2026-05-10 00:00:00', level: '北大核心', rankInfo: '第1作者', isCorresponding: 1, fundAmount: null, score: null, scoreStatus: 0, status: 0, auditRemark: null, createTime: '2026-08-25 15:00:00' }
]

const ACH_AUDIT = {
  1: [
    { auditId: 1, achId: 1, auditType: 'SUBMIT', auditName: '张伟', opinion: '提交审核', auditTime: '2026-03-20 09:12:00' },
    { auditId: 2, achId: 1, auditType: 'APPROVE', auditName: '李娜', opinion: '科室初审通过', auditTime: '2026-03-21 10:00:00' },
    { auditId: 3, achId: 1, auditType: 'APPROVE', auditName: '王强', opinion: '查重合规，予以入库', auditTime: '2026-03-22 14:00:00' }
  ],
  5: [{ auditId: 10, achId: 5, auditType: 'SUBMIT', auditName: '李娜', opinion: '提交审核', auditTime: '2026-08-20 09:00:00' }]
}
const ACH_ATTACH = {
  1: [{ fileId: 101, bizType: 'ACH', bizId: 1, fileName: '录用证明.pdf', filePath: 'mock', fileSize: 102400, fileType: 'pdf', uploadTime: '2026-03-20 09:10:00' }]
}

let RISK_LOGS = [
  { riskId: 1, userId: 2, achId: 2, riskType: 'BLACKLIST_JOURNAL', riskDesc: '期刊命中预警名单：Example Journal（来源 中科院预警名单2025）', matchValue: 'Example Journal', status: 'NEW', checkId: null, createTime: '2026-08-25 10:00:00' },
  { riskId: 2, userId: 3, achId: 5, riskType: 'TIME_LOGIC', riskDesc: '发表/立项时间晚于当前时间，时间逻辑异常', matchValue: '2027-01-01', status: 'NEW', checkId: null, createTime: '2026-08-26 09:00:00' }
]

let CHECKS = [
  { checkId: 1, checkNo: 'CK202608250001', userId: 2, achId: null, riskSource: 'MANUAL', riskType: 'MULTI_SUBMIT', riskDesc: '涉嫌一稿多投：同一 DOI 已在其他期刊发表', status: 'PENDING', assignee: null, claimTime: null, conclusion: null, deductScore: null, violationId: null, publicityId: null, finishTime: null, createTime: '2026-08-25 10:30:00' }
]
const CHECK_RECORDS = {
  1: [{ recordId: 1, checkId: 1, recordType: 'EVIDENCE', content: '系统比对：DOI 10.2000/x.2025.01.001 存在两篇标题相似论文', operatorName: '系统', operateTime: '2026-08-25 10:30:00' }]
}

let VIOLATIONS = [
  { violationId: 1, userId: 2, achId: null, checkId: 1, violationType: 'EXAGGERATION', violationLevel: 'C', deductScore: 15, description: '成果虚假夸大', status: 'EFFECTIVE', effectiveDate: '2026-08-01', vetoFlag: 0, reformDeadline: '2026-09-01', reformResult: null, createTime: '2026-08-01 09:00:00' }
]

let APPEALS = [
  { appealId: 1, appealNo: 'AP202608010001', userId: 2, appealType: 'DEDUCT', bizType: 'VIOLATION', bizId: 1, reason: '该成果已按要求补充证明材料，恳请复核扣分', status: 'REVIEWING', result: null, createTime: '2026-08-02 10:00:00' }
]

let NOTICES = [
  { noticeId: 1, userId: 2, noticeType: 'RISK', title: '科研诚信风险提醒', content: '您的成果《MicroRNA-21 在骨肉瘤中的表达及意义》触发风险筛查：期刊命中预警名单', isRead: 0, createTime: '2026-08-25 10:00:00' },
  { noticeId: 2, userId: 2, noticeType: 'EVALUATE', title: '扣分生效通知', content: '您的违规记录已生效（扣分 15 分），年度评价已更新', isRead: 0, createTime: '2026-08-01 09:05:00' },
  { noticeId: 3, userId: 2, noticeType: 'APPEAL', title: '申诉复核结果通知', content: '您的申诉（AP202608010001）复核结果：复核中', isRead: 1, createTime: '2026-08-02 10:05:00' }
]

let INTEGRITY = [
  { integrityId: 1, userId: 2, year: 2026, periodType: 'YEAR', perfScore: 143, deductScore: 15, totalScore: 128, level: 'C', vetoFlag: 0, ruleVersion: '1.0', calcStatus: 1, remark: null }
]
const INTEGRITY_DETAIL = [
  { detailId: 1, integrityId: 1, userId: 2, year: 2026, bizType: 'PERF', achId: 1, itemName: '关节置换术后快速康复的临床研究', baseScore: 15, coefficient: null, score: 15, ruleVersion: '1.0' },
  { detailId: 2, integrityId: 1, userId: 2, year: 2026, bizType: 'PERF', achId: 2, itemName: 'MicroRNA-21 在骨肉瘤中的表达及意义', baseScore: 50, coefficient: null, score: 50, ruleVersion: '1.0' },
  { detailId: 3, integrityId: 1, userId: 2, year: 2026, bizType: 'PERF', achId: 4, itemName: '一种可调式脊柱内固定装置', baseScore: 30, coefficient: null, score: 30, ruleVersion: '1.0' },
  { detailId: 4, integrityId: 1, userId: 2, year: 2026, bizType: 'DEDUCT', violationId: 1, itemName: 'EXAGGERATION（C级）', baseScore: 15, coefficient: null, score: -15, ruleVersion: '1.0' }
]

const RULES = [
  { ruleId: 1, ruleType: 'PERF', ruleNo: 'PERF-PAPER-01', achType: 'PAPER', ruleName: 'SCI/SSCI 一区论文', baseScore: 50, calcMode: 'FIXED', configJson: null, version: '1.0', effectiveDate: '2025-01-01', status: 1, vetoFlag: 0, needReform: 0 },
  { ruleId: 2, ruleType: 'PERF', ruleNo: 'PERF-PAPER-06', achType: 'PAPER', ruleName: 'EI期刊/北大中文核心论文', baseScore: 15, calcMode: 'FIXED', configJson: null, version: '1.0', effectiveDate: '2025-01-01', status: 1, vetoFlag: 0, needReform: 0 },
  { ruleId: 3, ruleType: 'PERF', ruleNo: 'PERF-TOPIC-01', achType: 'TOPIC', ruleName: '国家级课题', baseScore: null, calcMode: 'ROLE_MAP', configJson: '{"LEADER":80,"CORE":40,"MEMBER":16}', version: '1.0', effectiveDate: '2025-01-01', status: 1, vetoFlag: 0, needReform: 0 },
  { ruleId: 4, ruleType: 'DEDUCT', ruleNo: 'DEDUCT-C-01', achType: 'MULTI_SUBMIT', ruleName: '一稿多投', baseScore: 30, calcMode: 'FIXED', configJson: null, version: '1.0', effectiveDate: '2025-01-01', status: 1, vetoFlag: 0, needReform: 1 },
  { ruleId: 5, ruleType: 'DEDUCT', ruleNo: 'DEDUCT-D-01', achType: 'PLAGIARISM', ruleName: '抄袭剽窃', baseScore: 0, calcMode: 'FIXED', configJson: null, version: '1.0', effectiveDate: '2025-01-01', status: 1, vetoFlag: 1, needReform: 0 }
]
const COEFFS = [
  { coeffId: 1, ruleId: null, coeffType: 'RANK', positionKey: '1ST', positionLabel: '第一作者', coefficient: 1.0, sortOrder: 1 },
  { coeffId: 2, ruleId: null, coeffType: 'RANK', positionKey: '2ND', positionLabel: '第二作者', coefficient: 0.6, sortOrder: 2 },
  { coeffId: 3, ruleId: null, coeffType: 'REWARD_RANK', positionKey: '1', positionLabel: '第1名', coefficient: 1.0, sortOrder: 1 }
]
const LEVELS = [
  { levelId: 1, level: 'A', levelName: '诚信优秀', minDeduct: 0, maxDeduct: 0, vetoFlag: 0, conditions: '无扣分无风险', version: '1.0', effectiveDate: '2025-01-01', status: 1 },
  { levelId: 2, level: 'B', levelName: '诚信合格', minDeduct: 0, maxDeduct: 10, vetoFlag: 0, conditions: '轻微扣分且整改完成', version: '1.0', effectiveDate: '2025-01-01', status: 1 },
  { levelId: 3, level: 'C', levelName: '诚信警示', minDeduct: 10, maxDeduct: 30, vetoFlag: 0, conditions: '中度失信', version: '1.0', effectiveDate: '2025-01-01', status: 1 },
  { levelId: 4, level: 'D', levelName: '严重失信', minDeduct: 30, maxDeduct: 999999, vetoFlag: 1, conditions: '一票否决', version: '1.0', effectiveDate: '2025-01-01', status: 1 }
]
const BLACKLISTS = [
  { blId: 1, blType: 'JOURNAL', blName: 'Example Journal', source: '中科院预警名单2025', riskLevel: '高风险', status: 1, createTime: '2026-01-01 00:00:00' },
  { blId: 2, blType: 'JOURNAL', blName: 'Low Quality Press', source: '自定义', riskLevel: '中风险', status: 1, createTime: '2026-01-01 00:00:00' },
  { blId: 3, blType: 'KEYWORD', blName: '论文代写', source: '自定义', riskLevel: '高', status: 1, createTime: '2026-01-01 00:00:00' }
]
const DICT_TYPES = [
  { dictId: 1, dictName: '成果类型', dictType: 'ach_type', status: 1 },
  { dictId: 2, dictName: '核查工单状态', dictType: 'check_status', status: 1 },
  { dictId: 3, dictName: '违规等级', dictType: 'violation_level', status: 1 }
]
const DICT_DATA = {
  ach_type: [
    { dictCode: 1, dictType: 'ach_type', dictLabel: '期刊论文/会议论文', dictValue: 'PAPER', sortOrder: 1, status: 1 },
    { dictCode: 2, dictType: 'ach_type', dictLabel: '课题项目', dictValue: 'TOPIC', sortOrder: 2, status: 1 },
    { dictCode: 3, dictType: 'ach_type', dictLabel: '专利/软著', dictValue: 'PATENT', sortOrder: 3, status: 1 },
    { dictCode: 4, dictType: 'ach_type', dictLabel: '科技奖励', dictValue: 'REWARD', sortOrder: 4, status: 1 },
    { dictCode: 5, dictType: 'ach_type', dictLabel: '专著/教材', dictValue: 'BOOK', sortOrder: 5, status: 1 },
    { dictCode: 6, dictType: 'ach_type', dictLabel: '标准/指南', dictValue: 'STANDARD', sortOrder: 6, status: 1 },
    { dictCode: 7, dictType: 'ach_type', dictLabel: '学术任职', dictValue: 'POST', sortOrder: 7, status: 1 },
    { dictCode: 8, dictType: 'ach_type', dictLabel: '技术转化', dictValue: 'TRANSFER', sortOrder: 8, status: 1 }
  ],
  check_status: [
    { dictCode: 10, dictType: 'check_status', dictLabel: '待认领', dictValue: 'PENDING', sortOrder: 1, status: 1 },
    { dictCode: 11, dictType: 'check_status', dictLabel: '核查中', dictValue: 'PROCESSING', sortOrder: 2, status: 1 },
    { dictCode: 12, dictType: 'check_status', dictLabel: '已认定', dictValue: 'CONFIRMED', sortOrder: 3, status: 1 },
    { dictCode: 13, dictType: 'check_status', dictLabel: '已生效', dictValue: 'PUBLISHED', sortOrder: 4, status: 1 },
    { dictCode: 14, dictType: 'check_status', dictLabel: '误报撤销', dictValue: 'DISMISSED', sortOrder: 5, status: 1 }
  ],
  violation_level: [
    { dictCode: 20, dictType: 'violation_level', dictLabel: '轻微失信', dictValue: 'B', sortOrder: 1, status: 1 },
    { dictCode: 21, dictType: 'violation_level', dictLabel: '中度失信', dictValue: 'C', sortOrder: 2, status: 1 },
    { dictCode: 22, dictType: 'violation_level', dictLabel: '严重失信', dictValue: 'D', sortOrder: 3, status: 1 }
  ]
}

// ============================================================
// 工具
// ============================================================
const ok = (data) => ({ code: 0, msg: 'ok', data: data === undefined ? null : data })
const fail = (msg, code = 500) => ({ code, msg, data: null })
const pageOf = (list, pageNum, pageSize) => {
  pageNum = Number(pageNum) || 1
  pageSize = Number(pageSize) || 10
  return { total: list.length, list: list.slice((pageNum - 1) * pageSize, pageNum * pageSize) }
}
const userOf = (u) => ({
  userId: u.userId, username: u.username, realName: u.realName, deptId: u.deptId,
  deptName: (DEPTS.find((d) => d.deptId === u.deptId) || {}).deptName || '',
  title: u.title, roles: u.roles, perms: []
})
const roleOf = (username) => USERS.find((u) => u.username === username)
const menuTreeOf = (roles) => {
  const ids = new Set()
  roles.forEach((r) => (ROLE_MENUS[r] || []).forEach((id) => ids.add(id)))
  const pick = (nodes) => nodes
    .filter((m) => ids.has(m.menuId))
    .map((m) => ({ ...m, children: pick(m.children || []) }))
  return pick(MENUS)
}
const ACH_TYPE_LABEL = { PAPER: '论文', TOPIC: '课题', PATENT: '专利/软著', REWARD: '奖励', BOOK: '专著/教材', STANDARD: '标准/指南', POST: '学术任职', TRANSFER: '技术转化', OTHER: '其他' }

// ============================================================
// API 路由
// ============================================================
const api = express.Router()

// ---------- 认证 ----------
api.post('/auth/login', (req, res) => {
  const { username, password } = req.body || {}
  const user = roleOf(username)
  if (!user || PWD[username] !== password) {
    return res.json(fail('账号或密码错误', 400))
  }
  res.json(ok({ token: `mock-token-${user.username}`, user: userOf(user) }))
})
api.get('/auth/info', (req, res) => {
  const username = (req.headers.authorization || '').replace('Bearer mock-token-', '') || 'admin'
  const user = roleOf(username) || USERS[0]
  res.json(ok(userOf(user)))
})
api.post('/auth/logout', (req, res) => res.json(ok()))
api.put('/auth/password', (req, res) => res.json(ok()))

// ---------- 菜单 ----------
api.get('/system/menu/tree', (req, res) => {
  const username = (req.headers.authorization || '').replace('Bearer mock-token-', '') || 'admin'
  const user = roleOf(username) || USERS[0]
  res.json(ok(menuTreeOf(user.roles)))
})
api.get('/system/menu/all', (req, res) => res.json(ok(MENUS)))

// ---------- 看板 ----------
api.get('/dashboard/my', (req, res) => {
  res.json(ok({
    year: 2026, yearScore: 128, perfScore: 143, deductScore: 15, level: 'C', vetoFlag: 0,
    achStats: [
      { achType: 'PAPER', cnt: 2 }, { achType: 'TOPIC', cnt: 1 }, { achType: 'PATENT', cnt: 1 }, { achType: 'REWARD', cnt: 1 }
    ],
    deductList: VIOLATIONS.filter((v) => v.status !== 'REVOKED').slice(0, 10),
    pendingCount: ACH.filter((a) => [1, 2].includes(a.status)).length,
    riskCount: RISK_LOGS.filter((r) => r.status === 'NEW').length
  }))
})
api.get('/dashboard/dept', (req, res) => {
  res.json(ok({
    deptId: 2, achTotal: 4,
    typeDist: [{ achType: 'PAPER', cnt: 2 }, { achType: 'TOPIC', cnt: 1 }, { achType: 'PATENT', cnt: 1 }],
    levelDist: [{ level: 'C', cnt: 1 }],
    riskUsers: [{ userId: 2, cnt: 1 }],
    unAudited: 1
  }))
})
api.get('/dashboard/hospital', (req, res) => {
  res.json(ok({
    summary: { users: 126, achTotal: 458, avgScore: 86.5 },
    deptRank: [
      { deptName: '骨科', avgScore: 92.4, userCount: 18 },
      { deptName: '心内科', avgScore: 88.1, userCount: 22 },
      { deptName: '检验科', avgScore: 81.3, userCount: 15 }
    ],
    riskTypeStat: [
      { riskType: 'MULTI_SUBMIT', cnt: 6 }, { riskType: 'BLACKLIST_JOURNAL', cnt: 4 }, { riskType: 'TIME_LOGIC', cnt: 3 }
    ],
    yearTrend: [
      { year: 2022, cnt: 61 }, { year: 2023, cnt: 78 }, { year: 2024, cnt: 95 }, { year: 2025, cnt: 112 }, { year: 2026, cnt: 112 }
    ],
    seriousList: [{ realName: '张某', totalScore: 20, year: 2026 }],
    pendingChecks: 3
  }))
})

// ---------- 成果 ----------
api.get('/achievement/page', (req, res) => {
  let list = [...ACH]
  const { status, achType, keyword, year } = req.query
  if (status !== undefined && status !== '') list = list.filter((a) => String(a.status) === String(status))
  if (achType) list = list.filter((a) => a.achType === achType)
  if (keyword) list = list.filter((a) => (a.title || '').includes(keyword) || (a.achNo || '').includes(keyword))
  if (year) list = list.filter((a) => (a.publishTime || '').startsWith(String(year)))
  res.json(ok(pageOf(list, req.query.pageNum, req.query.pageSize)))
})
api.get('/achievement/audit/page', (req, res) => {
  const scope = req.query.scope || 'ALL'
  const status = scope === 'DEPT' ? 1 : 2
  res.json(ok(pageOf(ACH.filter((a) => a.status === status), req.query.pageNum, req.query.pageSize)))
})
api.get('/achievement/:id', (req, res) => {
  const ach = ACH.find((a) => a.achId === Number(req.params.id))
  if (!ach) return res.json(fail('成果不存在', 400))
  res.json(ok({
    achievement: ach,
    attachments: ACH_ATTACH[ach.achId] || [],
    auditLogs: ACH_AUDIT[ach.achId] || []
  }))
})
api.post('/achievement', (req, res) => {
  const body = req.body || {}
  const ach = {
    achId: nextId(), userId: 2, achType: body.achType, title: body.title, achNo: body.achNo || null,
    sourceName: body.sourceName || null, publishTime: body.publishTime || now(), level: body.level || null,
    rankInfo: body.rankInfo || null, isCorresponding: body.isCorresponding || 0, fundAmount: body.fundAmount || null,
    score: null, scoreStatus: 0, status: 0, auditRemark: null, createTime: now()
  }
  ACH.push(ach)
  res.json(ok(ach))
})
api.put('/achievement/:id', (req, res) => {
  const ach = ACH.find((a) => a.achId === Number(req.params.id))
  if (!ach) return res.json(fail('成果不存在', 400))
  Object.assign(ach, req.body || {})
  res.json(ok())
})
api.delete('/achievement/:id', (req, res) => {
  ACH = ACH.filter((a) => a.achId !== Number(req.params.id))
  res.json(ok())
})
api.post('/achievement/:id/submit', (req, res) => {
  const ach = ACH.find((a) => a.achId === Number(req.params.id))
  if (ach) { ach.status = 1; ACH_AUDIT[ach.achId] = [{ auditId: nextId(), achId: ach.achId, auditType: 'SUBMIT', auditName: '张伟', opinion: '提交审核', auditTime: now() }] }
  res.json(ok())
})
api.post('/achievement/:id/audit', (req, res) => {
  const ach = ACH.find((a) => a.achId === Number(req.params.id))
  if (!ach) return res.json(fail('成果不存在', 400))
  const { auditType, opinion } = req.body || {}
  if (auditType === 'APPROVE') {
    if (ach.status === 1) ach.status = 2
    else if (ach.status === 2) {
      ach.status = 3
      ach.score = ach.score === null || ach.score === undefined ? 10 : ach.score
      ach.scoreStatus = 1
      ach.auditRemark = opinion || null
    }
  } else if (auditType === 'BACK') {
    ach.status = 4
    ach.auditRemark = opinion || null
  }
  ACH_AUDIT[ach.achId] = ACH_AUDIT[ach.achId] || []
  ACH_AUDIT[ach.achId].push({ auditId: nextId(), achId: ach.achId, auditType, auditName: '王强', opinion: opinion || null, auditTime: now() })
  res.json(ok())
})
api.post('/achievement/:id/invalidate', (req, res) => {
  const ach = ACH.find((a) => a.achId === Number(req.params.id))
  if (ach) { ach.status = 6; ach.score = 0; ach.scoreStatus = 2 }
  res.json(ok())
})
api.get('/achievement/export', (req, res) => res.json(ok()))

// ---------- 文件 ----------
api.post('/file/upload', (req, res) => {
  res.json(ok({ fileId: nextId(), fileName: '上传附件.pdf', fileSize: 1024 }))
})
api.get('/file/download', (req, res) => {
  res.type('application/octet-stream')
  res.send(Buffer.from('mock file content'))
})
api.delete('/file/:id', (req, res) => res.json(ok()))

// ---------- 诚信评价 ----------
api.get('/integrity/my', (req, res) => {
  res.json(ok(INTEGRITY[0] || null))
})
api.get('/integrity/my/detail', (req, res) => res.json(ok(INTEGRITY_DETAIL)))
api.get('/integrity/page', (req, res) => {
  const rows = INTEGRITY.map((i) => {
    const u = USERS.find((x) => x.userId === i.userId)
    return { integrity: i, realName: u ? u.realName : '-', deptName: u ? ((DEPTS.find((d) => d.deptId === u.deptId) || {}).deptName || '') : '-' }
  })
  res.json(ok(pageOf(rows, req.query.pageNum, req.query.pageSize)))
})
api.get('/integrity/user/:userId', (req, res) => res.json(ok(INTEGRITY[0] || null)))
api.get('/integrity/user/:userId/detail', (req, res) => res.json(ok({ integrity: INTEGRITY[0] || null, details: INTEGRITY_DETAIL })))
api.post('/integrity/calc', (req, res) => {
  INTEGRITY[0].calcStatus = 2
  res.json(ok(6))
})
api.get('/integrity/export/pdf', (req, res) => {
  res.type('application/pdf')
  res.send(Buffer.from('%PDF-1.4 mock archive pdf'))
})
api.post('/integrity/:id/publicity', (req, res) => res.json(ok()))

// ---------- 风险与处置 ----------
api.get('/risk/log/page', (req, res) => res.json(ok(pageOf(RISK_LOGS, req.query.pageNum, req.query.pageSize))))
api.post('/risk/log/:id/claim', (req, res) => {
  const log = RISK_LOGS.find((r) => r.riskId === Number(req.params.id))
  if (log) {
    log.status = 'CLAIMED'
    const check = {
      checkId: nextId(), checkNo: `CK${now().replace(/[-: ]/g, '').slice(0, 14)}`, userId: log.userId, achId: log.achId,
      riskSource: 'AUTO', riskType: log.riskType, riskDesc: log.riskDesc, status: 'PROCESSING', assignee: 4,
      claimTime: now(), conclusion: null, deductScore: null, violationId: null, publicityId: null, finishTime: null, createTime: now()
    }
    CHECKS.push(check)
    log.checkId = check.checkId
    CHECK_RECORDS[check.checkId] = []
    res.json(ok(check))
  } else {
    res.json(fail('预警记录不存在', 400))
  }
})
api.get('/risk/check/page', (req, res) => res.json(ok(pageOf(CHECKS, req.query.pageNum, req.query.pageSize))))
api.get('/risk/check/:id', (req, res) => {
  const check = CHECKS.find((c) => c.checkId === Number(req.params.id))
  if (!check) return res.json(fail('工单不存在', 400))
  res.json(ok({
    check,
    records: CHECK_RECORDS[check.checkId] || [],
    violation: check.violationId ? VIOLATIONS.find((v) => v.violationId === check.violationId) : null
  }))
})
api.post('/risk/check/:id/claim', (req, res) => {
  const check = CHECKS.find((c) => c.checkId === Number(req.params.id))
  if (check && check.status === 'PENDING') { check.status = 'PROCESSING'; check.assignee = 4; check.claimTime = now() }
  res.json(ok())
})
api.post('/risk/check/:id/record', (req, res) => {
  const { recordType, content } = req.body || {}
  CHECK_RECORDS[req.params.id] = CHECK_RECORDS[req.params.id] || []
  CHECK_RECORDS[req.params.id].push({ recordId: nextId(), checkId: Number(req.params.id), recordType, content, operatorName: '王强', operateTime: now() })
  res.json(ok())
})
api.post('/risk/check/:id/confirm', (req, res) => {
  const check = CHECKS.find((c) => c.checkId === Number(req.params.id))
  if (check) {
    const body = req.body || {}
    const violation = {
      violationId: nextId(), userId: check.userId, achId: check.achId, checkId: check.checkId,
      violationType: body.violationType || 'MULTI_SUBMIT', violationLevel: body.violationLevel || 'C',
      deductScore: body.deductScore || 30, description: body.description || '', evidence: body.evidence || '',
      status: 'CONFIRMED', effectiveDate: null, vetoFlag: 0, reformDeadline: body.reformDeadline || '2026-09-15',
      reformResult: null, createTime: now()
    }
    VIOLATIONS.push(violation)
    check.status = 'CONFIRMED'
    check.conclusion = violation.description
    check.deductScore = violation.deductScore
    check.violationId = violation.violationId
  }
  res.json(ok())
})
api.post('/risk/check/:id/dismiss', (req, res) => {
  const check = CHECKS.find((c) => c.checkId === Number(req.params.id))
  if (check) { check.status = 'DISMISSED'; check.finishTime = now() }
  res.json(ok())
})
api.post('/risk/check/:id/publish', (req, res) => {
  const check = CHECKS.find((c) => c.checkId === Number(req.params.id))
  if (check && check.status === 'CONFIRMED') check.status = 'TO_PUBLIC'
  res.json(ok())
})
api.post('/risk/check/:id/effect', (req, res) => {
  const check = CHECKS.find((c) => c.checkId === Number(req.params.id))
  if (check) {
    check.status = 'PUBLISHED'
    check.finishTime = now()
    const v = VIOLATIONS.find((x) => x.violationId === check.violationId)
    if (v) { v.status = 'EFFECTIVE'; v.effectiveDate = now().slice(0, 10) }
  }
  res.json(ok())
})
api.post('/risk/check/:id/archive', (req, res) => {
  const check = CHECKS.find((c) => c.checkId === Number(req.params.id))
  if (check) check.status = 'ARCHIVED'
  res.json(ok())
})
api.get('/risk/violation/page', (req, res) => res.json(ok(pageOf(VIOLATIONS, req.query.pageNum, req.query.pageSize))))
api.post('/risk/violation/:id/reform', (req, res) => {
  const v = VIOLATIONS.find((x) => x.violationId === Number(req.params.id))
  if (v && v.status === 'EFFECTIVE') { v.status = 'REFORMING'; v.reformResult = (req.body || {}).result }
  res.json(ok())
})
api.post('/risk/violation/:id/reformCheck', (req, res) => {
  const v = VIOLATIONS.find((x) => x.violationId === Number(req.params.id))
  if (v && v.status === 'REFORMING') {
    v.status = (req.body || {}).pass ? 'REFORMED' : 'EFFECTIVE'
  }
  res.json(ok())
})

// ---------- 申诉 ----------
api.get('/appeal/page', (req, res) => res.json(ok(pageOf(APPEALS, req.query.pageNum, req.query.pageSize))))
api.post('/appeal', (req, res) => {
  const body = req.body || {}
  APPEALS.unshift({
    appealId: nextId(), appealNo: `AP${now().replace(/[-: ]/g, '').slice(0, 14)}`, userId: 2,
    appealType: body.appealType || 'DEDUCT', bizType: body.bizType || 'VIOLATION', bizId: body.bizId || 1,
    reason: body.reason || '', status: 'PENDING', result: null, createTime: now()
  })
  res.json(ok())
})
api.get('/appeal/:id', (req, res) => {
  const a = APPEALS.find((x) => x.appealId === Number(req.params.id))
  res.json(ok(a || null))
})
api.post('/appeal/:id/review', (req, res) => {
  const a = APPEALS.find((x) => x.appealId === Number(req.params.id))
  if (a) {
    a.status = (req.body || {}).pass || 'SUSTAINED'
    a.result = (req.body || {}).result || ''
  }
  res.json(ok())
})

// ---------- 通知 ----------
api.get('/notice/my', (req, res) => res.json(ok(pageOf(NOTICES, req.query.pageNum, req.query.pageSize))))
api.put('/notice/:id/read', (req, res) => {
  const n = NOTICES.find((x) => x.noticeId === Number(req.params.id))
  if (n) n.isRead = 1
  res.json(ok())
})
api.put('/notice/readAll', (req, res) => {
  NOTICES.forEach((n) => { n.isRead = 1 })
  res.json(ok())
})
api.get('/notice/unreadCount', (req, res) => res.json(ok(NOTICES.filter((n) => n.isRead === 0).length)))

// ---------- 系统管理 ----------
api.get('/system/user/page', (req, res) => {
  const rows = USERS.map((u) => ({
    userId: u.userId, empNo: u.empNo, username: u.username, realName: u.realName, deptId: u.deptId,
    deptName: ((DEPTS.find((d) => d.deptId === u.deptId) || {}).deptName) || '', title: u.title,
    phone: '138****0001', email: '', status: 1, lastLoginTime: now(), createTime: '2026-01-01 09:00:00',
    roleIds: [ROLES.find((r) => r.roleKey === u.roles[0]).roleId],
    roleNames: ROLES.filter((r) => u.roles.includes(r.roleKey)).map((r) => r.roleName).join('、')
  }))
  res.json(ok(pageOf(rows, req.query.pageNum, req.query.pageSize)))
})
api.post('/system/user', (req, res) => res.json(ok()))
api.put('/system/user/:id', (req, res) => res.json(ok()))
api.delete('/system/user/:id', (req, res) => res.json(ok()))
api.put('/system/user/:id/status', (req, res) => res.json(ok()))
api.put('/system/user/:id/resetPwd', (req, res) => res.json(ok()))
api.get('/system/dept/tree', (req, res) => res.json(ok(DEPTS)))
api.post('/system/dept', (req, res) => res.json(ok()))
api.put('/system/dept', (req, res) => res.json(ok()))
api.delete('/system/dept/:id', (req, res) => res.json(ok()))
api.get('/system/role/list', (req, res) => res.json(ok(ROLES)))
api.post('/system/role', (req, res) => res.json(ok()))
api.put('/system/role', (req, res) => res.json(ok()))
api.delete('/system/role/:id', (req, res) => res.json(ok()))
api.put('/system/role/:id/menus', (req, res) => res.json(ok()))
api.get('/system/role/:id/menus', (req, res) => res.json(ok([])))
api.post('/system/menu', (req, res) => res.json(ok()))
api.put('/system/menu', (req, res) => res.json(ok()))
api.delete('/system/menu/:id', (req, res) => res.json(ok()))
api.get('/system/dict/type/list', (req, res) => res.json(ok(DICT_TYPES)))
api.get('/system/dict/data/:dictType', (req, res) => res.json(ok(DICT_DATA[req.params.dictType] || [])))
api.post('/system/dict/type', (req, res) => res.json(ok()))
api.put('/system/dict/type', (req, res) => res.json(ok()))
api.delete('/system/dict/type/:id', (req, res) => res.json(ok()))
api.post('/system/dict/data', (req, res) => res.json(ok()))
api.put('/system/dict/data', (req, res) => res.json(ok()))
api.delete('/system/dict/data/:id', (req, res) => res.json(ok()))
api.get('/system/rule/page', (req, res) => res.json(ok(pageOf(RULES, req.query.pageNum, req.query.pageSize))))
api.post('/system/rule', (req, res) => res.json(ok()))
api.put('/system/rule/:id', (req, res) => res.json(ok()))
api.delete('/system/rule/:id', (req, res) => res.json(ok()))
api.get('/system/rule/coeff/list', (req, res) => res.json(ok(COEFFS)))
api.get('/system/rule/level/list', (req, res) => res.json(ok(LEVELS)))
api.get('/system/blacklist/page', (req, res) => res.json(ok(pageOf(BLACKLISTS, req.query.pageNum, req.query.pageSize))))
api.post('/system/blacklist', (req, res) => res.json(ok()))
api.put('/system/blacklist/:id', (req, res) => res.json(ok()))
api.delete('/system/blacklist/:id', (req, res) => res.json(ok()))
api.get('/system/log/page', (req, res) => {
  const logs = [
    { logId: 1, username: 'auditor', module: '风控', operation: '失信认定', method: 'confirm', ip: '10.1.1.5', status: 1, costTime: 23, createTime: '2026-08-26 10:00:00' },
    { logId: 2, username: 'admin', module: '系统', operation: '新增用户', method: 'create', ip: '10.1.1.8', status: 1, costTime: 12, createTime: '2026-08-25 16:30:00' }
  ]
  res.json(ok(pageOf(logs, req.query.pageNum, req.query.pageSize)))
})
api.get('/import/template', (req, res) => res.json(ok()))

// ============================================================
// 静态托管 + SPA 回退
// ============================================================
app.use('/api/v1', api)
app.use(express.static(DIST))
app.use((req, res, next) => {
  if (req.path.startsWith('/api')) return next()
  res.sendFile(path.join(DIST, 'index.html'))
})

app.listen(PORT, () => {
  console.log('')
  console.log('════════════════════════════════════════════════════════')
  console.log('  医院个人科研成果诚信综合评价系统 · 本地演示服务')
  console.log(`  访问地址：http://127.0.0.1:${PORT}`)
  console.log('  管理员：admin / admin123')
  console.log('  其他角色：doctor|dept|auditor|committee|leader / 123456')
  console.log('════════════════════════════════════════════════════════')
})
