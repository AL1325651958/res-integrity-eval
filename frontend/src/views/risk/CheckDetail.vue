<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">核查工单详情</span>
        <div class="header-right">
          <el-button @click="goBack">返回列表</el-button>
        </div>
      </div>

      <div v-loading="loading">
        <template v-if="check">
          <!-- 基本信息 -->
          <el-descriptions :column="3" border>
            <el-descriptions-item label="工单编号">{{ check.checkNo || check.id || '-' }}</el-descriptions-item>
            <el-descriptions-item label="涉及人员">{{ check.userName || check.realName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="checkStatusType(check.status)">{{ checkStatusLabel(check.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="风险类型">{{ RISK_TYPE_MAP[check.riskType] || check.riskType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="关联成果">{{ check.achievementTitle || check.title || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ check.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="风险描述" :span="3">{{ check.description || check.riskDesc || '-' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 认定信息 -->
          <template v-if="check.violation">
            <div class="section-title">认定信息</div>
            <el-descriptions :column="3" border>
              <el-descriptions-item label="违规类型">{{ VIOLATION_TYPE_MAP[check.violation.violationType] || check.violation.violationType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="违规等级">
                <el-tag :type="violationLevelType(check.violation.violationLevel)">{{ violationLevelLabel(check.violation.violationLevel) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="扣分">{{ check.violation.deductScore ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="认定描述" :span="2">{{ check.violation.description || '-' }}</el-descriptions-item>
              <el-descriptions-item label="整改期限">{{ check.violation.reformDeadline || '-' }}</el-descriptions-item>
              <el-descriptions-item label="认定证据" :span="3">{{ check.violation.evidence || '-' }}</el-descriptions-item>
            </el-descriptions>
          </template>

          <!-- 操作按钮（按状态流转） -->
          <div v-if="hasActions" class="action-bar">
            <template v-if="actions.claim">
              <el-button type="primary" :loading="actionLoading" @click="handleClaim">认领</el-button>
            </template>
            <template v-if="actions.record">
              <el-button type="primary" plain @click="openRecord">添加核查记录</el-button>
            </template>
            <template v-if="actions.confirm">
              <el-button type="danger" @click="openConfirm">认定</el-button>
            </template>
            <template v-if="actions.dismiss">
              <el-button type="info" plain @click="handleDismiss">误报撤销</el-button>
            </template>
            <template v-if="actions.publish">
              <el-button type="warning" @click="handlePublish">发起公示</el-button>
            </template>
            <template v-if="actions.effect">
              <el-button type="success" @click="handleEffect">公示结束生效</el-button>
            </template>
            <template v-if="actions.archive">
              <el-button type="info" @click="handleArchive">归档</el-button>
            </template>
          </div>

          <!-- 核查记录时间线 -->
          <div class="section-title">核查记录</div>
          <el-timeline v-if="records.length" class="record-timeline">
            <el-timeline-item v-for="(r, idx) in records" :key="idx" :timestamp="r.operateTime" placement="top">
              <div class="record-item">
                <el-tag :type="recordTypeTag(r.recordType)" size="small">{{ RECORD_TYPE_MAP[r.recordType] || r.recordType || '-' }}</el-tag>
                <span class="record-operator">{{ r.operatorName || '操作人' }}</span>
                <div class="record-content">{{ r.content || '-' }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无核查记录" :image-size="60" />
        </template>
        <el-empty v-if="!loading && !check" description="未获取到工单信息" />
      </div>
    </el-card>

    <!-- 添加核查记录 -->
    <el-dialog v-model="recordVisible" title="添加核查记录" width="520px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="recordFormRef" :model="recordForm" :rules="recordRules" label-width="90px">
        <el-form-item label="记录类型" prop="recordType">
          <el-radio-group v-model="recordForm.recordType">
            <el-radio value="EVIDENCE">取证</el-radio>
            <el-radio value="INVESTIGATE">调查</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="记录内容" prop="content">
          <el-input v-model="recordForm.content" type="textarea" :rows="4" placeholder="请填写取证/调查记录内容" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleAddRecord">保存</el-button>
      </template>
    </el-dialog>

    <!-- 认定 -->
    <el-dialog v-model="confirmVisible" title="失信认定" width="560px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="confirmFormRef" :model="confirmForm" :rules="confirmRules" label-width="100px">
        <el-form-item label="违规类型" prop="violationType">
          <el-select v-model="confirmForm.violationType" placeholder="请选择违规类型" style="width: 100%">
            <el-option v-for="(label, key) in VIOLATION_TYPE_MAP" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="违规等级" prop="violationLevel">
          <el-radio-group v-model="confirmForm.violationLevel">
            <el-radio value="SERIOUS">严重失信</el-radio>
            <el-radio value="MODERATE">中度失信</el-radio>
            <el-radio value="MINOR">轻微失信</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="扣分" prop="deductScore">
          <el-input-number v-model="confirmForm.deductScore" :min="0" :precision="2" style="width: 100%" placeholder="请输入扣分值" />
        </el-form-item>
        <el-form-item label="认定描述" prop="description">
          <el-input v-model="confirmForm.description" type="textarea" :rows="3" placeholder="认定事实与依据" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="认定证据" prop="evidence">
          <el-input v-model="confirmForm.evidence" type="textarea" :rows="3" placeholder="证据材料说明" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="整改期限" prop="reformDeadline">
          <el-date-picker v-model="confirmForm.reformDeadline" type="date" value-format="YYYY-MM-DD" placeholder="选择整改截止日期" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmVisible = false">取消</el-button>
        <el-button type="danger" :loading="actionLoading" @click="handleConfirm">确认认定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getCheck,
  claimCheck,
  addCheckRecord,
  confirmCheck,
  dismissCheck,
  publishCheck,
  effectCheck,
  archiveCheck,
} from '@/api/risk'

const route = useRoute()
const router = useRouter()
const checkId = String(route.params.id ?? '')

const CHECK_STATUS_MAP: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待认领', type: 'info' },
  PROCESSING: { label: '处理中', type: 'primary' },
  TO_CONFIRM: { label: '待认定', type: 'warning' },
  CONFIRMED: { label: '已认定', type: 'warning' },
  TO_PUBLIC: { label: '待公示', type: 'warning' },
  PUBLISHED: { label: '公示中', type: 'success' },
  ARCHIVED: { label: '已归档', type: 'info' },
  DISMISSED: { label: '已撤销', type: 'danger' },
}

const RISK_TYPE_MAP: Record<string, string> = {
  DUPLICATE_SUBMIT: '一稿多投预警',
  AUTHORSHIP: '署名异常预警',
  TIME_LOGIC: '时间逻辑预警',
  BLACKLIST_JOURNAL: '黑名单期刊预警',
  REPEAT_DECLARE: '重复申报预警',
  OTHER: '其他风险',
}

const RECORD_TYPE_MAP: Record<string, string> = {
  EVIDENCE: '取证',
  INVESTIGATE: '调查',
  CONFIRM: '认定',
  HANDLE: '处理',
}

const VIOLATION_TYPE_MAP: Record<string, string> = {
  PLAGIARISM: '抄袭剽窃',
  DATA_FABRICATION: '数据造假',
  EXPERIMENT_FABRICATION: '实验造假',
  FORGERY: '伪造证书/材料',
  PAPER_TRADING: '买卖论文',
  GHOSTWRITING: '代写代发',
  DUPLICATE_SUBMIT: '一稿多投',
  REPEAT_PUBLISH: '重复发表',
  IMPROPER_AUTHORSHIP: '不当署名',
  MISAPPROPRIATION: '侵占他人成果',
  EXAGGERATION: '成果虚假夸大',
  OTHER: '其他',
}

const VIOLATION_LEVEL_MAP: Record<string, { label: string; type: string }> = {
  SERIOUS: { label: '严重失信', type: 'danger' },
  MODERATE: { label: '中度失信', type: 'warning' },
  MINOR: { label: '轻微失信', type: 'primary' },
}

function checkStatusLabel(status: unknown): string {
  return CHECK_STATUS_MAP[String(status)]?.label || String(status ?? '-')
}
function checkStatusType(status: unknown): string {
  return CHECK_STATUS_MAP[String(status)]?.type || 'info'
}
function violationLevelLabel(level: unknown): string {
  return VIOLATION_LEVEL_MAP[String(level)]?.label || String(level ?? '-')
}
function violationLevelType(level: unknown): string {
  return VIOLATION_LEVEL_MAP[String(level)]?.type || 'info'
}
function recordTypeTag(type: unknown): string {
  const s = String(type ?? '')
  if (s === 'EVIDENCE') return 'primary'
  if (s === 'INVESTIGATE') return 'warning'
  if (s === 'CONFIRM') return 'danger'
  return 'info'
}

const check = ref<any>(null)
const records = ref<any[]>([])
const loading = ref(false)
const actionLoading = ref(false)

async function loadDetail() {
  loading.value = true
  try {
    const res: any = await getCheck(checkId)
    check.value = res ?? null
    records.value = Array.isArray(res?.records) ? res.records : []
  } catch (e: any) {
    ElMessage.error(e?.message || '获取工单详情失败')
  } finally {
    loading.value = false
  }
}

/** 按当前状态显示可用操作 */
const actions = computed(() => {
  const s = check.value?.status
  return {
    claim: s === 'PENDING',
    record: s === 'PROCESSING' || s === 'TO_CONFIRM',
    confirm: s === 'PROCESSING' || s === 'TO_CONFIRM',
    dismiss: s === 'PROCESSING' || s === 'TO_CONFIRM' || s === 'CONFIRMED',
    publish: s === 'CONFIRMED' || s === 'TO_PUBLIC',
    effect: s === 'PUBLISHED',
    archive: s === 'PUBLISHED',
  }
})
const hasActions = computed(() => Object.values(actions.value).some(Boolean))

function goBack() {
  router.push('/risk/check')
}

async function handleClaim() {
  actionLoading.value = true
  try {
    await claimCheck(checkId)
    ElMessage.success('已认领')
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e?.message || '认领失败')
  } finally {
    actionLoading.value = false
  }
}

/** 添加核查记录（取证/调查） */
const recordVisible = ref(false)
const recordFormRef = ref<FormInstance>()
const recordForm = reactive({ recordType: 'EVIDENCE', content: '' })
const recordRules = reactive<FormRules>({
  recordType: [{ required: true, message: '请选择记录类型', trigger: 'change' }],
  content: [{ required: true, message: '请填写记录内容', trigger: 'blur' }],
})

function openRecord() {
  recordForm.recordType = 'EVIDENCE'
  recordForm.content = ''
  recordVisible.value = true
}

async function handleAddRecord() {
  try {
    await recordFormRef.value?.validate()
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await addCheckRecord(checkId, { recordType: recordForm.recordType, content: recordForm.content })
    ElMessage.success('记录已添加')
    recordVisible.value = false
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    actionLoading.value = false
  }
}

/** 认定：生成违规记录 → 状态待公示 */
const confirmVisible = ref(false)
const confirmFormRef = ref<FormInstance>()
const confirmForm = reactive({
  violationType: '',
  violationLevel: 'SERIOUS',
  deductScore: undefined as number | undefined,
  description: '',
  evidence: '',
  reformDeadline: '',
})
const confirmRules = reactive<FormRules>({
  violationType: [{ required: true, message: '请选择违规类型', trigger: 'change' }],
  violationLevel: [{ required: true, message: '请选择违规等级', trigger: 'change' }],
  deductScore: [{ required: true, message: '请输入扣分值', trigger: 'blur' }],
  description: [{ required: true, message: '请填写认定描述', trigger: 'blur' }],
})

function openConfirm() {
  confirmForm.violationType = ''
  confirmForm.violationLevel = 'SERIOUS'
  confirmForm.deductScore = undefined
  confirmForm.description = ''
  confirmForm.evidence = ''
  confirmForm.reformDeadline = ''
  confirmVisible.value = true
}

async function handleConfirm() {
  try {
    await confirmFormRef.value?.validate()
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await confirmCheck(checkId, {
      violationType: confirmForm.violationType,
      violationLevel: confirmForm.violationLevel,
      deductScore: confirmForm.deductScore,
      description: confirmForm.description,
      evidence: confirmForm.evidence,
      reformDeadline: confirmForm.reformDeadline,
    })
    ElMessage.success('认定成功，已生成违规记录')
    confirmVisible.value = false
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e?.message || '认定失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleDismiss() {
  try {
    await ElMessageBox.confirm('确认该工单为误报并撤销？', '误报撤销', { type: 'warning' })
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await dismissCheck(checkId)
    ElMessage.success('已撤销（误报）')
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e?.message || '撤销失败')
  } finally {
    actionLoading.value = false
  }
}

async function handlePublish() {
  try {
    await ElMessageBox.confirm('确认发起失信认定公示？', '发起公示', { type: 'warning' })
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await publishCheck(checkId)
    ElMessage.success('已发起公示')
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e?.message || '发起公示失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleEffect() {
  try {
    await ElMessageBox.confirm('确认公示期结束，扣分生效？', '公示结束生效', { type: 'warning' })
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await effectCheck(checkId)
    ElMessage.success('扣分已生效')
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleArchive() {
  try {
    await ElMessageBox.confirm('确认归档该工单？归档后不可再操作。', '归档确认', { type: 'warning' })
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await archiveCheck(checkId)
    ElMessage.success('已归档')
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e?.message || '归档失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 20px 0 12px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
.action-bar {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
.record-timeline {
  padding-left: 4px;
}
.record-item {
  font-size: 13px;
}
.record-operator {
  margin-left: 8px;
  color: #606266;
}
.record-content {
  margin-top: 6px;
  color: #303133;
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
}
</style>
