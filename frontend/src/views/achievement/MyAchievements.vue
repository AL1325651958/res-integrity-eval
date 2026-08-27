<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">我的科研成果</span>
        <el-button type="primary" @click="openCreate">新增成果</el-button>
      </div>

      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="成果状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px">
            <el-option v-for="(item, key) in ACH_STATUS_MAP" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="成果类型">
          <el-select v-model="query.achType" placeholder="全部类型" clearable style="width: 160px">
            <el-option v-for="(label, key) in ACH_TYPE_MAP" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="年度">
          <el-date-picker v-model="query.year" type="year" value-format="YYYY" placeholder="选择年度" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="标题/编号" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="title" label="成果标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="成果类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ ACH_TYPE_MAP[row.achType] || row.achType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别/分区" width="130" show-overflow-tooltip />
        <el-table-column prop="publishTime" label="发表/立项时间" width="130" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="achStatusType(row.status)">{{ achStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button v-if="row.status === '0'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === '0'" link type="primary" @click="handleSubmit(row)">提交审核</el-button>
            <el-button v-if="row.status === '0'" link type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button v-if="row.status === '3' && isAdmin" link type="warning" @click="handleInvalidate(row)">作废</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无成果数据" />
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

    <AchievementFormDialog v-model="formVisible" :achievement-id="editingId" :ach-type-preset="typePreset" @success="loadList" />

    <el-drawer v-model="detailVisible" title="成果详情" size="560px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="成果标题" :span="2">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="成果类型">{{ ACH_TYPE_MAP[detail.achType] || detail.achType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ achStatusLabel(detail.status) }}</el-descriptions-item>
            <el-descriptions-item label="编号">{{ detail.achNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="来源/单位">{{ detail.sourceName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="级别/分区">{{ detail.level || '-' }}</el-descriptions-item>
            <el-descriptions-item label="作者排名">{{ detail.rankInfo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="是否通讯/一作">{{ detail.isCorresponding ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="经费(万元)">{{ detail.fundAmount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="发表/立项时间">{{ detail.publishTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">附件列表</div>
          <div v-if="detail.attachments && detail.attachments.length" class="attach-list">
            <div v-for="att in detail.attachments" :key="att.fileId" class="attach-item">
              <el-link type="primary" :underline="false" @click="downloadAtt(att)">{{ att.fileName }}</el-link>
            </div>
          </div>
          <el-empty v-else description="暂无附件" :image-size="60" />

          <div class="section-title">审核流水</div>
          <el-timeline v-if="detail.auditLogs && detail.auditLogs.length">
            <el-timeline-item v-for="(log, idx) in detail.auditLogs" :key="idx" :timestamp="log.auditTime" placement="top">
              <div class="timeline-item">
                <el-tag :type="log.auditType === 'APPROVE' ? 'success' : 'danger'" size="small">
                  {{ log.auditType === 'APPROVE' ? '通过' : '退回' }}
                </el-tag>
                <span class="timeline-name">{{ log.auditName || '审核人' }}</span>
                <div v-if="log.opinion" class="timeline-opinion">{{ log.opinion }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审核记录" :image-size="60" />
        </template>
        <el-empty v-if="!detailLoading && !detail" description="未获取到成果详情" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageAchievements,
  getAchievement,
  deleteAchievement,
  submitAchievement,
  invalidateAchievement,
} from '@/api/achievement'
import { downloadFile } from '@/api/file'
import AchievementFormDialog from './AchievementForm.vue'

/** 成果状态：0草稿 1待科室初审 2待科研科终审 3已入库 4已退回 5已撤销 6已作废 */
const ACH_STATUS_MAP: Record<string, { label: string; type: string }> = {
  '0': { label: '草稿', type: 'info' },
  '1': { label: '待科室初审', type: 'warning' },
  '2': { label: '待科研科终审', type: 'warning' },
  '3': { label: '已入库', type: 'success' },
  '4': { label: '已退回', type: 'danger' },
  '5': { label: '已撤销', type: 'info' },
  '6': { label: '已作废', type: 'danger' },
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

function achStatusLabel(status: unknown): string {
  return ACH_STATUS_MAP[String(status)]?.label || String(status ?? '-')
}

function achStatusType(status: unknown): string {
  return ACH_STATUS_MAP[String(status)]?.type || 'info'
}

/** 从本地缓存读取当前用户角色（兼容不同存储键），未取到视为无该角色 */
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

const isAdmin = computed(() => hasRole('admin'))

interface Query {
  pageNum: number
  pageSize: number
  status?: string
  achType?: string
  keyword?: string
  year?: string
}

const query = reactive<Query>({ pageNum: 1, pageSize: 10, status: undefined, achType: undefined, keyword: '', year: undefined })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, any> = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.status) params.status = query.status
    if (query.achType) params.achType = query.achType
    if (query.keyword) params.keyword = query.keyword
    if (query.year) params.year = query.year
    const res: any = await pageAchievements(params)
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
  query.achType = undefined
  query.keyword = ''
  query.year = undefined
  handleSearch()
}

/** 新增/编辑弹窗 */
const formVisible = ref(false)
const editingId = ref<number | null>(null)
const typePreset = ref<string | undefined>(undefined)

function openCreate() {
  editingId.value = null
  typePreset.value = undefined
  formVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.id
  typePreset.value = row.achType
  formVisible.value = true
}

/** 提交审核（草稿 → 待科室初审） */
async function handleSubmit(row: any) {
  try {
    await ElMessageBox.confirm(`确认将成果「${row.title}」提交审核？`, '提交审核', { type: 'warning' })
  } catch {
    return
  }
  try {
    await submitAchievement(row.id)
    ElMessage.success('已提交，等待科室初审')
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  }
}

/** 删除草稿 */
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除草稿「${row.title}」？删除后不可恢复。`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteAchievement(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

/** 作废（管理员角色）：回收计分并触发重算 */
async function handleInvalidate(row: any) {
  try {
    await ElMessageBox.confirm(
      `确认作废成果「${row.title}」？作废后将回收已计分值并触发诚信评分重算。`,
      '作废确认',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await invalidateAchievement(row.id)
    ElMessage.success('已作废并回收计分')
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '作废失败')
  }
}

/** 详情抽屉 */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

async function openDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res: any = await getAchievement(row.id)
    detail.value = res ?? row
  } catch (e: any) {
    ElMessage.error(e?.message || '获取详情失败')
  } finally {
    detailLoading.value = false
  }
}

/** 触发浏览器下载（兼容 api 返回 Blob 或响应包装） */
function downloadBlob(res: any, filename: string) {
  let blob: Blob | null = null
  if (res instanceof Blob) blob = res
  else if (res?.data instanceof Blob) blob = res.data
  if (blob) {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } else if (typeof res === 'string') {
    window.open(res, '_blank')
  } else {
    ElMessage.warning('下载失败：未获取到文件流')
  }
}

async function downloadAtt(att: any) {
  try {
    const res: any = await downloadFile(att.fileId)
    downloadBlob(res, att.fileName || '附件')
  } catch (e: any) {
    ElMessage.error(e?.message || '下载失败')
  }
}

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
.filter-form {
  margin-bottom: 4px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 20px 0 12px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
.attach-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.timeline-item {
  font-size: 13px;
}
.timeline-name {
  margin-left: 8px;
  color: #606266;
}
.timeline-opinion {
  margin-top: 4px;
  color: #909399;
  background: #f5f7fa;
  padding: 6px 10px;
  border-radius: 4px;
}
</style>
