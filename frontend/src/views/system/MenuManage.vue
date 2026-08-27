<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">菜单管理</span>
        <el-button type="primary" @click="openCreate(null)">新增菜单</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tree"
        row-key="menuId"
        default-expand-all
        :tree-props="{ children: 'children' }"
        stripe
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="220" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="menuTypeTag(row.menuType)">{{ menuTypeLabel(row.menuType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由地址" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.path || '-' }}</template>
        </el-table-column>
        <el-table-column prop="component" label="组件路径" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.component || '-' }}</template>
        </el-table-column>
        <el-table-column prop="perms" label="权限标识" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.perms || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center">
          <template #default="{ row }">{{ row.sort ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="menuStatus(row.status) === '启用' ? 'success' : 'danger'">{{ menuStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">新增下级</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无菜单数据" />
        </template>
      </el-table>
    </el-card>

    <!-- 新增/编辑菜单 -->
    <el-dialog v-model="formVisible" :title="editingId ? '编辑菜单' : '新增菜单'" width="560px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-select v-model="form.parentId" placeholder="无（顶级菜单）" clearable style="width: 100%">
            <el-option v-for="m in menuOptions" :key="m.menuId" :label="m._label" :value="m.menuId" :disabled="m.menuId === editingId" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="菜单/按钮名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'F'" label="路由地址">
          <el-input v-model="form.path" placeholder="如：/achievement/my" maxlength="200" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 'C'" label="组件路径">
          <el-input v-model="form.component" placeholder="如：achievement/MyAchievements" maxlength="200" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 'F'" label="权限标识">
          <el-input v-model="form.perms" placeholder="如：achievement:audit" maxlength="100" />
        </el-form-item>
        <el-form-item label="排序">
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
import { menuTree, saveMenu, deleteMenu } from '@/api/system'

/** 菜单类型：M 目录 / C 菜单 / F 按钮 */
const MENU_TYPE_MAP: Record<string, { label: string; type: string }> = {
  M: { label: '目录', type: 'warning' },
  C: { label: '菜单', type: 'primary' },
  F: { label: '按钮', type: 'info' },
}

function menuTypeLabel(type: unknown): string {
  return MENU_TYPE_MAP[String(type)]?.label || String(type ?? '-')
}
function menuTypeTag(type: unknown): string {
  return MENU_TYPE_MAP[String(type)]?.type || 'info'
}

function menuStatus(status: unknown): string {
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
    const res: any = await menuTree()
    tree.value = Array.isArray(res) ? res : res?.list ?? []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载菜单失败')
  } finally {
    loading.value = false
  }
}

/** 上级菜单选项 */
const menuOptions = ref<{ menuId: number | string; _label: string }[]>([])
function flattenMenus(nodes: any[], level = 0) {
  nodes.forEach((n) => {
    menuOptions.value.push({ menuId: n.menuId, _label: `${'\u3000'.repeat(level)}${n.menuName}` })
    if (n.children && n.children.length) flattenMenus(n.children, level + 1)
  })
}
function rebuildOptions() {
  menuOptions.value = []
  flattenMenus(tree.value)
}

/** 新增/编辑 */
const formVisible = ref(false)
const editingId = ref<number | string | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

interface MenuForm {
  parentId?: number | string
  menuName: string
  menuType: string
  path: string
  component: string
  perms: string
  sort: number
  status: string
}
const form = reactive<MenuForm>({ parentId: undefined, menuName: '', menuType: 'C', path: '', component: '', perms: '', sort: 0, status: '0' })

const formRules = reactive<FormRules>({
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择类型', trigger: 'change' }],
})

function openCreate(parent: any) {
  editingId.value = null
  form.parentId = parent?.menuId
  form.menuName = ''
  form.menuType = 'C'
  form.path = ''
  form.component = ''
  form.perms = ''
  form.sort = 0
  form.status = '0'
  formVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.menuId
  form.parentId = row.parentId ?? undefined
  form.menuName = row.menuName || ''
  form.menuType = row.menuType || 'C'
  form.path = row.path || ''
  form.component = row.component || ''
  form.perms = row.perms || ''
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
    menuName: form.menuName,
    menuType: form.menuType,
    sort: form.sort,
    status: form.status,
  }
  if (form.parentId !== undefined && form.parentId !== null && form.parentId !== '') payload.parentId = form.parentId
  if (form.menuType !== 'F') payload.path = form.path
  if (form.menuType === 'C') payload.component = form.component
  if (form.menuType === 'F') payload.perms = form.perms
  if (editingId.value) payload.menuId = editingId.value
  saving.value = true
  try {
    await saveMenu(payload)
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
    await ElMessageBox.confirm(`确认删除菜单「${row.menuName}」？存在下级菜单时无法删除。`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteMenu(row.menuId)
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
