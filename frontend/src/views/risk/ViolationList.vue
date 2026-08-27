<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">违规记录</span>
      </div>

      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px" @change="handleSearch">
            <el-option v-for="(item, key) in VIOLATION_STATUS_MAP" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="违规等级">
          <el-select v-model="query.violationLevel" placeholder="全部等级" clearable style="width: 160px" @change="handleSearch">
            <el-option v-for="(item, key) in VIOLATION_LEVEL_MAP" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="涉及人员" width="120">
          <template #default="{ row }">{{ row.userName || row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column label="违规类型" width="150">
          <template #default="{ row }">
            <el-tag type="danger">{{ VIOLATION_TYPE_MAP[row.violationType] || row.violationType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="违规等级" width="120">
          <template #default="{ row }">
            <el-tag :type="violationLevelType(row.violationLevel)">{{ violationLevelLabel(row.violationLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="扣分" width="90" align="right">
          <template #default="{ row }">
            <span class="deduct-color">{{ row.deductScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="违规描述" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column prop="reformDeadline" label="整改期限" width="120">
          <template #default="{ row }">{{ row.reformDeadline || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="violationStatusType(row.status)">{{ violationStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="primary" @click="openReform(row)">提交整改</el-button>
            <el-button v-if="row.status === 'REFORMED' && isAuditor" link type="warning" @click="openReformCheck(row)">整改验收</el-button>
            <span v-if="!canOperate(row.status)" class="no-op">-</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无违规记录" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 提交整改 -->
    <el-dialog v-model="reformVisible" title="提交整改" width="520px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="reformFormRef" :model="reformForm" :rules="reformRules" label-width="90px">
        <el-form-item label="整改情况" prop="result">
          <el-input v-model="reformForm.result" type="textarea" :rows="5" placeholder="请填写整改情况说明" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reformVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleReform">提交</el-button>
      </template>
    </el-dialog>

    <!-- 整改验收 -->
    <el-dialog v-model="checkVisible" title="整改验收" width="520px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="checkFormRef" :model="checkForm" :rules="checkRules" label-width="90px">
        <el-form-item label="验收结论" prop="pass">
          <el-radio-group v-model="checkForm.pass">
            <el-radio :value="true">验收通过（按规则减免）</el-radio>
            <el-radio :value="false">验收不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="验收意见" prop="comment">
          <el-input v-model="checkForm.comment" type="textarea" :rows="4" placeholder="请填写验收意见" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleReformCheck">提交验收</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { violationPage, reformViolation, reformCheckViolation } from '@/api/risk'

/** 违规记录状态 */
const VIOLATION_STATUS_MAP: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待整改', type: 'danger' },
  REFORMED: { label: '待验收', type: 'warning' },
  PASSED: { label: '验收通过', type: 'success' },
  FAILED: { label: '验收不通过', type: 'danger' },
  CLOSED: { label: '已结案', type: 'info' },
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

function violationStatusLabel(status: unknown): string {
  return VIOLATION_STATUS_MAP[String(status)]?.label || String(status ?? '-')
}
function violationStatusType(status: unknown): string {
  return VIOLATION_STATUS_MAP[String(status)]?.type || 'info'
}
function violationLevelLabel(level: unknown): string {
  return VIOLATION_LEVEL_MAP[String(level)]?.label || String(level ?? '-')
}
function violationLevelType(level: unknown): string {
  return VIOLATION_LEVEL_MAP[String(level)]?.type || 'info'
}

function canOperate(status: unknown): boolean {
  const s = String(status)
  return s === 'PENDING' || s === 'REFORMED'
}

function hasRole(role: string): boolean {
  try {
    const keys = ['userInfo', 'user', 'USER_INFO', 'auth_user']
    for (const key of keys) {
      const raw = localStorage.getItem(key)
      if (!raw) continue
      const obj = JSON.parse(raw)
      const roles: unknown = obj?.roles ?? obj?.roleIds ?? obj?.roleKey
      if (Array.isArray(roles)) return roles.some((r: string) => String(r).toLowerCase() === role.toLowerCase())
      if (typeof roles === 'string') return roles.toLowerCase() === role.toLowerCase()
    }
  } catch {
    /* 忽略解析异常 */
  }
  return false
}

const isAuditor = computed(() => hasRole('auditor') || hasRole('admin'))

interface Query {
  pageNum: number
  pageSize: number
  status?: string
  violationLevel?: string
}
const query = reactive<Query>({ pageNum: 1, pageSize: 10, status: undefined, violationLevel: undefined })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, any> = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.status) params.status = query.status
    if (query.violationLevel) params.violationLevel = query.violationLevel
    const res: any = await violationPage(params)
    list.value = Array.isArray(res) ? res : res?.list ?? []
    total.value = res?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadList()
}

function handleReset() {
  query.status = undefined
  query.violationLevel = undefined
  handleSearch()
}

/** 提交整改 */
const reformVisible = ref(false)
const reformFormRef = ref<FormInstance>()
const currentRow = ref<any>(null)
const reformForm = reactive({ result: '' })
const reformRules = reactive<FormRules>({
  result: [{ required: true, message: '请填写整改情况', trigger: 'blur' }],
})

function openReform(row: any) {
  currentRow.value = row
  reformForm.result = ''
  reformVisible.value = true
}

async function handleReform() {
  try {
    await reformFormRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await reformViolation(currentRow.value.id, { result: reformForm.result })
    ElMessage.success('整改情况已提交')
    reformVisible.value = false
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

/** 整改验收 */
const checkVisible = ref(false)
const checkFormRef = ref<FormInstance>()
const checkForm = reactive({ pass: true, comment: '' })
const checkRules = reactive<FormRules>({
  pass: [{ required: true, message: '请选择验收结论', trigger: 'change' }],
  comment: [{ required: true, message: '请填写验收意见', trigger: 'blur' }],
})

function openReformCheck(row: any) {
  currentRow.value = row
  checkForm.pass = true
  checkForm.comment = ''
  checkVisible.value = true
}

async function handleReformCheck() {
  try {
    await checkFormRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await reformCheckViolation(currentRow.value.id, { pass: checkForm.pass, comment: checkForm.comment })
    ElMessage.success(checkForm.pass ? '验收通过' : '验收不通过')
    checkVisible.value = false
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '验收失败')
  } finally {
    submitting.value = false
  }
}

const submitting = ref(false)

onMounted(loadList)
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.filter-form {
  margin-bottom: 4px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.deduct-color {
  color: #f56c6c;
  font-weight: 600;
}
.no-op {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
