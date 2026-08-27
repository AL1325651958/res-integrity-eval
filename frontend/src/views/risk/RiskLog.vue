<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">风险预警记录</span>
      </div>

      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px" @change="handleSearch">
            <el-option v-for="(item, key) in RISK_STATUS_MAP" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险类型">
          <el-select v-model="query.riskType" placeholder="全部类型" clearable style="width: 180px" @change="handleSearch">
            <el-option v-for="(label, key) in RISK_TYPE_MAP" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="riskType" label="风险类型" width="150">
          <template #default="{ row }">
            <el-tag :type="riskTypeTag(row.riskType)">{{ RISK_TYPE_MAP[row.riskType] || row.riskType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="涉及人员" width="120">
          <template #default="{ row }">{{ row.userName || row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="description" label="风险描述" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || row.riskDesc || row.title || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="riskStatusType(row.status)">{{ riskStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="预警时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canClaim(row.status)" link type="primary" @click="handleClaim(row)">认领并转工单</el-button>
            <span v-else class="claim-done">已处置</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无风险预警记录" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { riskLogPage, claimRiskLog } from '@/api/risk'

/** 预警状态 */
const RISK_STATUS_MAP: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待认领', type: 'warning' },
  CLAIMED: { label: '已认领', type: 'primary' },
  PROCESSING: { label: '处理中', type: 'primary' },
  CLOSED: { label: '已处置', type: 'success' },
  DISMISSED: { label: '已撤销', type: 'info' },
}

/** 风险类型 */
const RISK_TYPE_MAP: Record<string, string> = {
  DUPLICATE_SUBMIT: '一稿多投预警',
  AUTHORSHIP: '署名异常预警',
  TIME_LOGIC: '时间逻辑预警',
  BLACKLIST_JOURNAL: '黑名单期刊预警',
  REPEAT_DECLARE: '重复申报预警',
  OTHER: '其他风险',
}

function riskStatusLabel(status: unknown): string {
  return RISK_STATUS_MAP[String(status)]?.label || String(status ?? '-')
}
function riskStatusType(status: unknown): string {
  return RISK_STATUS_MAP[String(status)]?.type || 'info'
}

function riskTypeTag(riskType: unknown): string {
  const s = String(riskType ?? '')
  if (s.includes('BLACKLIST') || s.includes('REPEAT')) return 'danger'
  if (s.includes('TIME')) return 'warning'
  return 'primary'
}

/** 仅待认领可转工单 */
function canClaim(status: unknown): boolean {
  return String(status) === 'PENDING'
}

interface Query {
  pageNum: number
  pageSize: number
  status?: string
  riskType?: string
}
const query = reactive<Query>({ pageNum: 1, pageSize: 10, status: undefined, riskType: undefined })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, any> = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.status) params.status = query.status
    if (query.riskType) params.riskType = query.riskType
    const res: any = await riskLogPage(params)
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
  query.riskType = undefined
  handleSearch()
}

async function handleClaim(row: any) {
  try {
    await ElMessageBox.confirm('确认认领该预警并转为核查工单？', '认领确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await claimRiskLog(row.id)
    ElMessage.success('已认领并转工单，可在核查工单中处理')
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '认领失败')
  }
}

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
.claim-done {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
