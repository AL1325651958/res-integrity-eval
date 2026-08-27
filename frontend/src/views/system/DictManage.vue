<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">字典管理</span>
        <el-button type="primary" @click="openTypeCreate">新增字典类型</el-button>
      </div>

      <el-row :gutter="16">
        <!-- 左：字典类型列表 -->
        <el-col :span="10">
          <div class="panel-title">字典类型</div>
          <el-table
            v-loading="typeLoading"
            :data="typeList"
            highlight-current-row
            stripe
            height="560"
            @current-change="handleTypeSelect"
          >
            <el-table-column prop="dictName" label="字典名称" min-width="130" show-overflow-tooltip />
            <el-table-column prop="dictType" label="字典类型" min-width="120" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="dictStatus(row.status) === '启用' ? 'success' : 'danger'" size="small">{{ dictStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="openTypeEdit(row)">编辑</el-button>
                <el-button link type="danger" @click.stop="handleTypeDelete(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无字典类型" :image-size="60" />
            </template>
          </el-table>
        </el-col>

        <!-- 右：字典数据 -->
        <el-col :span="14">
          <div class="panel-title">
            <span>{{ currentType ? `字典数据 · ${currentType.dictName}` : '字典数据' }}</span>
            <el-button type="primary" size="small" :disabled="!currentType" @click="openDataCreate">新增字典项</el-button>
          </div>
          <el-table v-loading="dataLoading" :data="dataList" stripe height="560">
            <el-table-column prop="dictLabel" label="字典标签" min-width="130" show-overflow-tooltip />
            <el-table-column prop="dictValue" label="字典键值" min-width="110" show-overflow-tooltip />
            <el-table-column prop="dictSort" label="排序" width="80" align="center">
              <template #default="{ row }">{{ row.dictSort ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="dictStatus(row.status) === '启用' ? 'success' : 'danger'" size="small">{{ dictStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDataEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="handleDataDelete(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="请选择左侧字典类型查看数据" :image-size="60" />
            </template>
          </el-table>
        </el-col>
      </el-row>
    </el-card>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeFormVisible" :title="typeEditingId ? '编辑字典类型' : '新增字典类型'" width="480px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeFormRules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="如：成果类型" maxlength="50" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="如：ach_type" maxlength="50" :disabled="!!typeEditingId" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="typeForm.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="3" placeholder="备注说明（选填）" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleTypeSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog v-model="dataFormVisible" :title="dataEditingId ? '编辑字典项' : '新增字典项'" width="480px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataFormRules" label-width="90px">
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="显示名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="存储值" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataForm.dictSort" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="dataForm.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="3" placeholder="备注说明（选填）" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleDataSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  dictTypeList,
  dictDataList,
  saveDictType,
  deleteDictType,
  saveDictData,
  deleteDictData,
} from '@/api/system'

function dictStatus(status: unknown): string {
  const s = String(status ?? '0')
  if (s === '0' || s === 'ENABLED' || s === 'NORMAL') return '启用'
  if (s === '1' || s === 'DISABLED' || s === 'FORBIDDEN') return '禁用'
  return s
}

/** 左侧字典类型 */
const typeList = ref<any[]>([])
const typeLoading = ref(false)
const currentType = ref<any>(null)

async function loadTypes() {
  typeLoading.value = true
  try {
    const res: any = await dictTypeList()
    typeList.value = Array.isArray(res) ? res : res?.list ?? []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载字典类型失败')
  } finally {
    typeLoading.value = false
  }
}

function handleTypeSelect(row: any) {
  currentType.value = row
  loadData(row.dictType)
}

/** 右侧字典数据 */
const dataList = ref<any[]>([])
const dataLoading = ref(false)

async function loadData(dictType?: string) {
  if (!dictType) {
    dataList.value = []
    return
  }
  dataLoading.value = true
  try {
    const res: any = await dictDataList(dictType)
    dataList.value = Array.isArray(res) ? res : res?.list ?? []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载字典数据失败')
  } finally {
    dataLoading.value = false
  }
}

/** 字典类型新增/编辑 */
const typeFormVisible = ref(false)
const typeEditingId = ref<number | null>(null)
const saving = ref(false)
const typeFormRef = ref<FormInstance>()

const typeForm = reactive({ dictName: '', dictType: '', status: '0', remark: '' })
const typeFormRules = reactive<FormRules>({
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }],
})

function openTypeCreate() {
  typeEditingId.value = null
  Object.assign(typeForm, { dictName: '', dictType: '', status: '0', remark: '' })
  typeFormVisible.value = true
}

function openTypeEdit(row: any) {
  typeEditingId.value = row.dictId ?? row.id
  typeForm.dictName = row.dictName || ''
  typeForm.dictType = row.dictType || ''
  typeForm.status = String(row.status ?? '0')
  typeForm.remark = row.remark || ''
  typeFormVisible.value = true
}

async function handleTypeSave() {
  try {
    await typeFormRef.value?.validate()
  } catch {
    return
  }
  const payload: Record<string, any> = {
    dictName: typeForm.dictName,
    dictType: typeForm.dictType,
    status: typeForm.status,
    remark: typeForm.remark,
  }
  if (typeEditingId.value) payload.dictId = typeEditingId.value
  saving.value = true
  try {
    await saveDictType(payload)
    ElMessage.success('保存成功')
    typeFormVisible.value = false
    loadTypes()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleTypeDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除字典类型「${row.dictName}」？其下字典数据将一并删除。`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteDictType(row.dictId ?? row.id)
    ElMessage.success('删除成功')
    if (currentType.value?.dictType === row.dictType) {
      currentType.value = null
      dataList.value = []
    }
    loadTypes()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

/** 字典数据新增/编辑 */
const dataFormVisible = ref(false)
const dataEditingId = ref<number | null>(null)
const dataFormRef = ref<FormInstance>()

const dataForm = reactive({ dictLabel: '', dictValue: '', dictSort: 0, status: '0', remark: '' })
const dataFormRules = reactive<FormRules>({
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }],
})

function openDataCreate() {
  if (!currentType.value) return
  dataEditingId.value = null
  Object.assign(dataForm, { dictLabel: '', dictValue: '', dictSort: 0, status: '0', remark: '' })
  dataFormVisible.value = true
}

function openDataEdit(row: any) {
  dataEditingId.value = row.dictId ?? row.id
  dataForm.dictLabel = row.dictLabel || ''
  dataForm.dictValue = row.dictValue || ''
  dataForm.dictSort = row.dictSort ?? 0
  dataForm.status = String(row.status ?? '0')
  dataForm.remark = row.remark || ''
  dataFormVisible.value = true
}

async function handleDataSave() {
  try {
    await dataFormRef.value?.validate()
  } catch {
    return
  }
  const payload: Record<string, any> = {
    dictType: currentType.value.dictType,
    dictLabel: dataForm.dictLabel,
    dictValue: dataForm.dictValue,
    dictSort: dataForm.dictSort,
    status: dataForm.status,
    remark: dataForm.remark,
  }
  if (dataEditingId.value) payload.dictId = dataEditingId.value
  saving.value = true
  try {
    await saveDictData(payload)
    ElMessage.success('保存成功')
    dataFormVisible.value = false
    loadData(currentType.value.dictType)
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDataDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除字典项「${row.dictLabel}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteDictData(row.dictId ?? row.id)
    ElMessage.success('删除成功')
    loadData(currentType.value?.dictType)
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(loadTypes)
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
.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}
</style>
