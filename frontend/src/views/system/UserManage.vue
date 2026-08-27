<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">用户管理</span>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>

      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="姓名/账号/工号" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="handleSearch">
            <el-option label="启用" value="0" />
            <el-option label="禁用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="工号" width="110">
          <template #default="{ row }">{{ row.userNo || row.empNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="deptName" label="科室" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deptName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="title" label="职称" width="110">
          <template #default="{ row }">{{ row.title || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <template v-if="row.roles && row.roles.length">
              <el-tag v-for="r in row.roles" :key="typeof r === 'string' ? r : r.roleKey" size="small" class="role-tag">
                {{ typeof r === 'string' ? r : r.roleName || r.roleKey }}
              </el-tag>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="userStatus(row.status) === '启用' ? 'success' : 'danger'">{{ userStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button link :type="userStatus(row.status) === '启用' ? 'warning' : 'success'" @click="handleToggleStatus(row)">
              {{ userStatus(row.status) === '启用' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户数据" />
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

    <!-- 新增/编辑用户 -->
    <el-dialog v-model="formVisible" :title="editingId ? '编辑用户' : '新增用户'" width="600px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="账号" prop="username">
              <el-input v-model="form.username" placeholder="登录账号" maxlength="50" :disabled="!!editingId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="!editingId" label="初始密码" prop="password">
              <el-input v-model="form.password" placeholder="初始密码" show-password maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="真实姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工号" prop="userNo">
              <el-input v-model="form.userNo" placeholder="工号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属科室" prop="deptId">
              <el-select v-model="form.deptId" placeholder="选择科室" clearable filterable style="width: 100%">
                <el-option v-for="d in deptOptions" :key="d.deptId" :label="d._label" :value="d.deptId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称" prop="title">
              <el-input v-model="form.title" placeholder="职称" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width: 100%">
                <el-option v-for="r in roleOptions" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
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
import {
  userPage,
  createUser,
  updateUser,
  deleteUser,
  updateUserStatus,
  resetUserPwd,
  deptTree,
  roleList,
} from '@/api/system'

function userStatus(status: unknown): string {
  const s = String(status ?? '0')
  if (s === '0' || s === 'ENABLED' || s === 'NORMAL') return '启用'
  if (s === '1' || s === 'DISABLED' || s === 'FORBIDDEN') return '禁用'
  return s
}

interface Query {
  pageNum: number
  pageSize: number
  keyword?: string
  status?: string
}
const query = reactive<Query>({ pageNum: 1, pageSize: 10, keyword: '', status: undefined })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, any> = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== undefined && query.status !== '') params.status = query.status
    const res: any = await userPage(params)
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
  query.keyword = ''
  query.status = undefined
  handleSearch()
}

/** 科室树 → 缩进选项 */
interface DeptOption {
  deptId: number | string
  _label: string
}
const deptOptions = ref<DeptOption[]>([])

function flattenDepts(nodes: any[], level = 0) {
  nodes.forEach((n) => {
    deptOptions.value.push({ deptId: n.deptId, _label: `${'\u3000'.repeat(level)}${n.deptName}` })
    if (n.children && n.children.length) flattenDepts(n.children, level + 1)
  })
}

async function loadDeptTree() {
  try {
    const res: any = await deptTree()
    const tree = Array.isArray(res) ? res : res?.list ?? []
    deptOptions.value = []
    flattenDepts(tree)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载科室失败')
  }
}

const roleOptions = ref<any[]>([])
async function loadRoles() {
  try {
    const res: any = await roleList()
    roleOptions.value = Array.isArray(res) ? res : res?.list ?? []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载角色失败')
  }
}

/** 新增/编辑弹窗 */
const formVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

interface UserForm {
  username: string
  password: string
  realName: string
  userNo: string
  deptId?: number | string
  title: string
  phone: string
  roleIds: (number | string)[]
}
const form = reactive<UserForm>({
  username: '',
  password: '',
  realName: '',
  userNo: '',
  deptId: undefined,
  title: '',
  phone: '',
  roleIds: [],
})

const formRules = reactive<FormRules>({
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
})

function openCreate() {
  editingId.value = null
  Object.assign(form, { username: '', password: '', realName: '', userNo: '', deptId: undefined, title: '', phone: '', roleIds: [] })
  formVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.id ?? row.userId
  form.username = row.username || ''
  form.password = ''
  form.realName = row.realName || ''
  form.userNo = row.userNo || row.empNo || ''
  form.deptId = row.deptId ?? undefined
  form.title = row.title || ''
  form.phone = row.phone || ''
  form.roleIds = Array.isArray(row.roleIds)
    ? row.roleIds
    : Array.isArray(row.roles)
      ? row.roles.map((r: any) => (typeof r === 'string' ? r : r.roleId)).filter(Boolean)
      : []
  formVisible.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const payload: Record<string, any> = {
    username: form.username,
    realName: form.realName,
    userNo: form.userNo,
    title: form.title,
    phone: form.phone,
    roleIds: form.roleIds,
  }
  if (form.deptId !== undefined && form.deptId !== null && form.deptId !== '') payload.deptId = form.deptId
  if (!editingId.value && form.password) payload.password = form.password
  saving.value = true
  try {
    if (editingId.value) {
      await updateUser(editingId.value, payload)
    } else {
      await createUser(payload)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

/** 启用/禁用切换 */
async function handleToggleStatus(row: any) {
  const next = userStatus(row.status) === '启用' ? '1' : '0'
  const actionText = next === '1' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${actionText}用户「${row.realName || row.username}」？`, `${actionText}确认`, { type: 'warning' })
  } catch {
    return
  }
  try {
    await updateUserStatus(row.id ?? row.userId, next)
    ElMessage.success(`已${actionText}`)
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

/** 重置密码 */
async function handleResetPwd(row: any) {
  try {
    await ElMessageBox.confirm(`确认重置用户「${row.realName || row.username}」的密码为初始密码？`, '重置密码', { type: 'warning' })
  } catch {
    return
  }
  try {
    await resetUserPwd(row.id ?? row.userId)
    ElMessage.success('密码已重置为初始密码')
  } catch (e: any) {
    ElMessage.error(e?.message || '重置失败')
  }
}

/** 删除用户 */
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.realName || row.username}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteUser(row.id ?? row.userId)
    ElMessage.success('删除成功')
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(() => {
  loadList()
  loadDeptTree()
  loadRoles()
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
.filter-form {
  margin-bottom: 4px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.role-tag {
  margin-right: 4px;
}
</style>
