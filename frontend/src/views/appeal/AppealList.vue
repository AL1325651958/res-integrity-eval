<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">我的申诉</span>
        <div class="header-right">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px" @change="handleSearch">
            <el-option v-for="(item, key) in APPEAL_STATUS_MAP" :key="key" :label="item.label" :value="key" />
          </el-select>
          <el-button type="primary" @click="openCreate">提交申诉</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="申诉类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ APPEAL_TYPE_MAP[row.appealType] || row.appealType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申诉对象" width="160">
          <template #default="{ row }">{{ bizTypeLabel(row.bizType) }} #{{ row.bizId ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="申诉理由" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="appealStatusType(row.status)">{{ appealStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="复核结果" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.result || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING' && isCommittee"
              link
              type="warning"
              @click="openReview(row)"
            >
              复核
            </el-button>
            <span v-else class="no-op">-</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无申诉记录" />
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

    <!-- 提交申诉 -->
    <el-dialog v-model="createVisible" title="提交申诉" width="520px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="申诉类型" prop="appealType">
          <el-select v-model="createForm.appealType" placeholder="请选择申诉类型" style="width: 100%">
            <el-option v-for="(label, key) in APPEAL_TYPE_MAP" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="对象类型" prop="bizType">
          <el-select v-model="createForm.bizType" placeholder="请选择对象类型" style="width: 100%">
            <el-option label="年度评分" value="INTEGRITY" />
            <el-option v-for="(label, key) in ACH_TYPE_MAP" :key="key" :label="`成果·${label}`" :value="key" />
            <el-option label="违规认定" value="VIOLATION" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="对象ID" prop="bizId">
          <el-input v-model="createForm.bizId" placeholder="请输入申诉对象的业务ID" />
        </el-form-item>
        <el-form-item label="申诉理由" prop="reason">
          <el-input v-model="createForm.reason" type="textarea" :rows="4" placeholder="请详细说明申诉理由" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">提交</el-button>
      </template>
    </el-dialog>

    <!-- 委员会复核 -->
    <el-dialog v-model="reviewVisible" title="复核裁定" width="520px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="100px">
        <el-form-item label="申诉内容">
          <div class="review-target">
            <p class="review-reason">{{ currentRow?.reason || '-' }}</p>
          </div>
        </el-form-item>
        <el-form-item label="裁定结果" prop="pass">
          <el-radio-group v-model="reviewForm.pass">
            <el-radio value="SUSTAINED">维持原判</el-radio>
            <el-radio value="OVERTURNED">申诉成立</el-radio>
            <el-radio value="REJECTED">驳回申诉</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="裁定意见" prop="result">
          <el-input v-model="reviewForm.result" type="textarea" :rows="4" placeholder="请填写复核裁定意见" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleReview">确定裁定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageAppeals, createAppeal, reviewAppeal } from '@/api/appeal'

const APPEAL_TYPE_MAP: Record<string, string> = {
  SCORE: '评分申诉',
  DEDUCT: '扣分申诉',
  CONFIRM: '认定申诉',
}

/** 申诉状态：待复核 / 复核后结论与 review 的 pass 取值一致 */
const APPEAL_STATUS_MAP: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待复核', type: 'warning' },
  SUSTAINED: { label: '维持原判', type: 'info' },
  OVERTURNED: { label: '申诉成立', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
}

const ACH_TYPE_MAP: Record<string, string> = {
  PAPER: '论文',
  TOPIC: '课题',
  PATENT: '专利',
  REWARD: '奖励',
  BOOK: '专著教材',
  STANDARD: '标准',
  POST: '学术任职',
  TRANSFER: '成果转化',
}

function appealStatusLabel(status: unknown): string {
  return APPEAL_STATUS_MAP[String(status)]?.label || String(status ?? '-')
}
function appealStatusType(status: unknown): string {
  return APPEAL_STATUS_MAP[String(status)]?.type || 'info'
}

function bizTypeLabel(bizType: unknown): string {
  const s = String(bizType ?? '')
  return ACH_TYPE_MAP[s] || s || '-'
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

const isCommittee = computed(() => hasRole('committee') || hasRole('admin'))

interface Query {
  pageNum: number
  pageSize: number
  status?: string
}
const query = reactive<Query>({ pageNum: 1, pageSize: 10, status: undefined })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, any> = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.status) params.status = query.status
    const res: any = await pageAppeals(params)
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

/** 提交申诉 */
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ appealType: '', bizType: '', bizId: '', reason: '' })
const createRules = reactive<FormRules>({
  appealType: [{ required: true, message: '请选择申诉类型', trigger: 'change' }],
  bizType: [{ required: true, message: '请选择对象类型', trigger: 'change' }],
  reason: [{ required: true, message: '请填写申诉理由', trigger: 'blur' }],
})

function openCreate() {
  createForm.appealType = ''
  createForm.bizType = ''
  createForm.bizId = ''
  createForm.reason = ''
  createVisible.value = true
}

async function handleCreate() {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await createAppeal({
      appealType: createForm.appealType,
      bizType: createForm.bizType,
      bizId: createForm.bizId,
      reason: createForm.reason,
    })
    ElMessage.success('申诉已提交，等待委员会复核')
    createVisible.value = false
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

/** 委员会复核 */
const reviewVisible = ref(false)
const reviewFormRef = ref<FormInstance>()
const currentRow = ref<any>(null)
const reviewForm = reactive({ pass: 'SUSTAINED', result: '' })
const reviewRules = reactive<FormRules>({
  pass: [{ required: true, message: '请选择裁定结果', trigger: 'change' }],
  result: [{ required: true, message: '请填写裁定意见', trigger: 'blur' }],
})

function openReview(row: any) {
  currentRow.value = row
  reviewForm.pass = 'SUSTAINED'
  reviewForm.result = ''
  reviewVisible.value = true
}

async function handleReview() {
  try {
    await reviewFormRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await reviewAppeal(currentRow.value.id, { pass: reviewForm.pass, result: reviewForm.result })
    ElMessage.success('复核裁定已提交')
    reviewVisible.value = false
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '裁定失败')
  } finally {
    submitting.value = false
  }
}

const submitting = ref(false)

onMounted(loadList)
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
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.no-op {
  color: #c0c4cc;
  font-size: 13px;
}
.review-target {
  width: 100%;
}
.review-reason {
  margin: 0;
  color: #606266;
  font-size: 13px;
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
}
</style>
