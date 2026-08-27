<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">黑名单管理</span>
        <el-button type="primary" @click="openCreate">新增黑名单</el-button>
      </div>

      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="类型">
          <el-select v-model="query.blType" placeholder="全部类型" clearable style="width: 160px" @change="handleSearch">
            <el-option v-for="(label, key) in BL_TYPE_MAP" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="名称/编号" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="类型" width="140">
          <template #default="{ row }">
            <el-tag :type="blTypeTag(row.blType)">{{ BL_TYPE_MAP[row.blType] || row.blType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.name || row.blName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="blNo" label="编号/ISSN" width="150">
          <template #default="{ row }">{{ row.blNo || row.issn || '-' }}</template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险等级" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.riskLevel" type="danger" size="small">{{ row.riskLevel }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="blStatus(row.status) === '启用' ? 'success' : 'danger'">{{ blStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无黑名单数据" />
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

    <!-- 新增/编辑 -->
    <el-dialog v-model="formVisible" :title="editingId ? '编辑黑名单' : '新增黑名单'" width="520px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="类型" prop="blType">
          <el-select v-model="form.blType" placeholder="选择类型" style="width: 100%">
            <el-option v-for="(label, key) in BL_TYPE_MAP" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="期刊/出版社等名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="编号/ISSN">
          <el-input v-model="form.blNo" placeholder="编号或 ISSN（选填）" maxlength="50" />
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="form.riskLevel" placeholder="选择风险等级" clearable style="width: 100%">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注说明（选填）" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { blacklistPage, saveBlacklist, deleteBlacklist } from '@/api/system'

const BL_TYPE_MAP: Record<string, string> = {
  JOURNAL: '预警期刊',
  PUBLISHER: '预警出版社',
  OTHER: '其他',
}

function blTypeTag(blType: unknown): string {
  const s = String(blType ?? '')
  if (s.includes('JOURNAL')) return 'danger'
  if (s.includes('PUBLISHER')) return 'warning'
  return 'info'
}

function blStatus(status: unknown): string {
  const s = String(status ?? '0')
  if (s === '0' || s === 'ENABLED' || s === 'NORMAL') return '启用'
  if (s === '1' || s === 'DISABLED' || s === 'FORBIDDEN') return '禁用'
  return s
}

interface Query {
  pageNum: number
  pageSize: number
  blType?: string
  keyword?: string
}
const query = reactive<Query>({ pageNum: 1, pageSize: 10, blType: undefined, keyword: '' })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, any> = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.blType) params.blType = query.blType
    if (query.keyword) params.keyword = query.keyword
    const res: any = await blacklistPage(params)
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
  query.blType = undefined
  query.keyword = ''
  handleSearch()
}

/** 新增/编辑 */
const formVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

interface BlacklistForm {
  blType: string
  name: string
  blNo: string
  riskLevel: string
  status: string
  remark: string
}
const form = reactive<BlacklistForm>({ blType: 'JOURNAL', name: '', blNo: '', riskLevel: '', status: '0', remark: '' })

const formRules = reactive<FormRules>({
  blType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
})

function openCreate() {
  editingId.value = null
  Object.assign(form, { blType: 'JOURNAL', name: '', blNo: '', riskLevel: '', status: '0', remark: '' })
  formVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.blacklistId ?? row.id
  form.blType = row.blType || 'JOURNAL'
  form.name = row.name || row.blName || ''
  form.blNo = row.blNo || row.issn || ''
  form.riskLevel = row.riskLevel || ''
  form.status = String(row.status ?? '0')
  form.remark = row.remark || ''
  formVisible.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const payload: Record<string, any> = {
    blType: form.blType,
    name: form.name,
    blNo: form.blNo,
    riskLevel: form.riskLevel,
    status: form.status,
    remark: form.remark,
  }
  if (editingId.value) payload.blacklistId = editingId.value
  saving.value = true
  try {
    await saveBlacklist(payload)
    ElMessage.success('保存成功')
    formVisible.value = false
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除黑名单「${row.name || row.blName}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteBlacklist(row.blacklistId ?? row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
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
</style>
