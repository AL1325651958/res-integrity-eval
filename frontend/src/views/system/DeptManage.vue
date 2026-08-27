<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">科室管理</span>
        <el-button type="primary" @click="openCreate(null)">新增科室</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tree"
        row-key="deptId"
        default-expand-all
        :tree-props="{ children: 'children' }"
        stripe
      >
        <el-table-column prop="deptName" label="科室名称" min-width="240" />
        <el-table-column prop="deptCode" label="科室编码" width="140">
          <template #default="{ row }">{{ row.deptCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" align="center">
          <template #default="{ row }">{{ row.sort ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="deptStatus(row.status) === '启用' ? 'success' : 'danger'">{{ deptStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">新增下级</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无科室数据" />
        </template>
      </el-table>
    </el-card>

    <!-- 新增/编辑科室 -->
    <el-dialog v-model="formVisible" :title="editingId ? '编辑科室' : '新增科室'" width="520px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="上级科室">
          <el-select v-model="form.parentId" placeholder="无（顶级科室）" clearable style="width: 100%">
            <el-option v-for="d in deptOptions" :key="d.deptId" :label="d._label" :value="d.deptId" :disabled="d.deptId === editingId" />
          </el-select>
        </el-form-item>
        <el-form-item label="科室名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入科室名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="科室编码" prop="deptCode">
          <el-input v-model="form.deptCode" placeholder="请输入科室编码" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
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
import { deptTree, saveDept, deleteDept } from '@/api/system'

function deptStatus(status: unknown): string {
  const s = String(status ?? '0')
  if (s === '0' || s === 'ENABLED' || s === 'NORMAL') return '启用'
  if (s === '1' || s === 'DISABLED' || s === 'FORBIDDEN') return '禁用'
  return s
}

const tree = ref<any[]>([])
const loading = ref(false)

async function loadTree() {
  loading.value = true
  try {
    const res: any = await deptTree()
    tree.value = Array.isArray(res) ? res : res?.list ?? []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载科室失败')
  } finally {
    loading.value = false
  }
}

/** 上级科室选择（去掉自身，避免选自己为父级） */
const deptOptions = ref<{ deptId: number | string; _label: string }[]>([])
function flattenDepts(nodes: any[], level = 0) {
  nodes.forEach((n) => {
    deptOptions.value.push({ deptId: n.deptId, _label: `${'\u3000'.repeat(level)}${n.deptName}` })
    if (n.children && n.children.length) flattenDepts(n.children, level + 1)
  })
}

function rebuildOptions() {
  deptOptions.value = []
  flattenDepts(tree.value)
}

/** 新增/编辑 */
const formVisible = ref(false)
const editingId = ref<number | string | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

interface DeptForm {
  parentId?: number | string
  deptName: string
  deptCode: string
  sort: number
  status: string
}
const form = reactive<DeptForm>({ parentId: undefined, deptName: '', deptCode: '', sort: 0, status: '0' })

const formRules = reactive<FormRules>({
  deptName: [{ required: true, message: '请输入科室名称', trigger: 'blur' }],
  deptCode: [{ required: true, message: '请输入科室编码', trigger: 'blur' }],
})

function openCreate(parent: any) {
  editingId.value = null
  form.parentId = parent?.deptId
  form.deptName = ''
  form.deptCode = ''
  form.sort = 0
  form.status = '0'
  formVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.deptId
  form.parentId = row.parentId ?? undefined
  form.deptName = row.deptName || ''
  form.deptCode = row.deptCode || ''
  form.sort = row.sort ?? 0
  form.status = String(row.status ?? '0')
  formVisible.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const payload: Record<string, any> = {
    deptName: form.deptName,
    deptCode: form.deptCode,
    sort: form.sort,
    status: form.status,
  }
  if (form.parentId !== undefined && form.parentId !== null && form.parentId !== '') payload.parentId = form.parentId
  if (editingId.value) payload.deptId = editingId.value
  saving.value = true
  try {
    await saveDept(payload)
    ElMessage.success('保存成功')
    formVisible.value = false
    loadTree()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除科室「${row.deptName}」？存在下级科室或关联用户时无法删除。`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteDept(row.deptId)
    ElMessage.success('删除成功')
    loadTree()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(() => {
  loadTree().then(rebuildOptions)
})
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
</style>
