<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">角色管理</span>
        <el-button type="primary" @click="openCreate">新增角色</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="roleName" label="角色名称" min-width="160" />
        <el-table-column prop="roleKey" label="角色标识" width="160">
          <template #default="{ row }">{{ row.roleKey || row.code || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="roleStatus(row.status) === '启用' ? 'success' : 'danger'">{{ roleStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="openMenus(row)">分配菜单</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无角色数据" />
        </template>
      </el-table>
    </el-card>

    <!-- 新增/编辑角色 -->
    <el-dialog v-model="formVisible" :title="editingId ? '编辑角色' : '新增角色'" width="480px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如：科研科审核员" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="如：auditor" maxlength="50" />
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

    <!-- 分配菜单 -->
    <el-dialog v-model="menuVisible" :title="`分配菜单 · ${currentRow?.roleName || ''}`" width="480px" destroy-on-close :close-on-click-modal="false">
      <div v-loading="menuLoading">
        <el-tree
          ref="menuTreeRef"
          :data="menuTreeData"
          show-checkbox
          node-key="menuId"
          default-expand-all
          :props="{ label: 'menuName', children: 'children' }"
        />
      </div>
      <template #footer>
        <el-button @click="menuVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveMenus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { ElTree } from 'element-plus'
import { roleList, saveRole, deleteRole, saveRoleMenus, menuTree } from '@/api/system'

function roleStatus(status: unknown): string {
  const s = String(status ?? '0')
  if (s === '0' || s === 'ENABLED' || s === 'NORMAL') return '启用'
  if (s === '1' || s === 'DISABLED' || s === 'FORBIDDEN') return '禁用'
  return s
}

const list = ref<any[]>([])
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res: any = await roleList()
    list.value = Array.isArray(res) ? res : res?.list ?? []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载角色失败')
  } finally {
    loading.value = false
  }
}

/** 新增/编辑 */
const formVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

interface RoleForm {
  roleName: string
  roleKey: string
  status: string
  remark: string
}
const form = reactive<RoleForm>({ roleName: '', roleKey: '', status: '0', remark: '' })

const formRules = reactive<FormRules>({
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
})

function openCreate() {
  editingId.value = null
  Object.assign(form, { roleName: '', roleKey: '', status: '0', remark: '' })
  formVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.roleId
  form.roleName = row.roleName || ''
  form.roleKey = row.roleKey || row.code || ''
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
    roleName: form.roleName,
    roleKey: form.roleKey,
    status: form.status,
    remark: form.remark,
  }
  if (editingId.value) payload.roleId = editingId.value
  saving.value = true
  try {
    await saveRole(payload)
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
    await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteRole(row.roleId)
    ElMessage.success('删除成功')
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

/** 分配菜单 */
const menuVisible = ref(false)
const menuLoading = ref(false)
const currentRow = ref<any>(null)
const menuTreeRef = ref<InstanceType<typeof ElTree>>()
const menuTreeData = ref<any[]>([])

async function openMenus(row: any) {
  currentRow.value = row
  menuVisible.value = true
  menuLoading.value = true
  try {
    const res: any = await menuTree()
    menuTreeData.value = Array.isArray(res) ? res : res?.list ?? []
    // 回显已分配菜单
    await menuTreeRef.value?.setCheckedKeys([])
    if (Array.isArray(row.menuIds) && row.menuIds.length) {
      menuTreeRef.value?.setCheckedKeys(row.menuIds.map(String))
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载菜单失败')
  } finally {
    menuLoading.value = false
  }
}

async function handleSaveMenus() {
  const tree = menuTreeRef.value
  if (!tree) return
  const checked = tree.getCheckedKeys() as (string | number)[]
  const halfChecked = tree.getHalfCheckedKeys() as (string | number)[]
  const menuIds = [...checked, ...halfChecked]
  saving.value = true
  try {
    await saveRoleMenus(currentRow.value.roleId, menuIds)
    ElMessage.success('菜单分配已保存')
    menuVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
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
</style>
