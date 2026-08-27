<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">核查工单</span>
      </div>

      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="工单状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px" @change="handleSearch">
            <el-option v-for="(item, key) in CHECK_STATUS_MAP" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" stripe @row-click="openDetail">
        <el-table-column prop="checkNo" label="工单编号" width="150">
          <template #default="{ row }">{{ row.checkNo || row.id || '-' }}</template>
        </el-table-column>
        <el-table-column label="涉及人员" width="120">
          <template #default="{ row }">{{ row.userName || row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column label="风险类型" width="150">
          <template #default="{ row }">
            <el-tag>{{ RISK_TYPE_MAP[row.riskType] || row.riskType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="风险描述" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || row.riskDesc || row.title || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="checkStatusType(row.status)">{{ checkStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无核查工单" />
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
import { useRouter } from 'vue-router'
import { checkPage } from '@/api/risk'

/** 工单状态流转：待认领 → 处理中 → 待认定 → 已认定 → 待公示 → 公示中 → 已归档 / 已撤销 */
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

function checkStatusLabel(status: unknown): string {
  return CHECK_STATUS_MAP[String(status)]?.label || String(status ?? '-')
}
function checkStatusType(status: unknown): string {
  return CHECK_STATUS_MAP[String(status)]?.type || 'info'
}

interface Query {
  pageNum: number
  pageSize: number
  status?: string
}
const query = reactive<Query>({ pageNum: 1, pageSize: 10, status: undefined })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

const router = useRouter()

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, any> = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.status) params.status = query.status
    const res: any = await checkPage(params)
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
  handleSearch()
}

function openDetail(row: any) {
  router.push(`/risk/check/${row.id}`)
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
</style>
