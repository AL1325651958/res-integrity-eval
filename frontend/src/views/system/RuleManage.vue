<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">评分规则配置</span>
      </div>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- 业绩规则 -->
        <el-tab-pane label="业绩规则" name="ACH">
          <div class="tab-toolbar">
            <el-select v-model="ruleQuery.achType" placeholder="全部成果类型" clearable style="width: 180px" @change="handleRuleSearch">
              <el-option v-for="(label, key) in ACH_TYPE_MAP" :key="key" :label="label" :value="key" />
            </el-select>
            <el-button type="primary" @click="openRuleCreate('ACH')">新增业绩规则</el-button>
          </div>
          <el-table v-loading="ruleLoading" :data="ruleList" stripe>
            <el-table-column prop="ruleNo" label="规则编号" width="120">
              <template #default="{ row }">{{ row.ruleNo || '-' }}</template>
            </el-table-column>
            <el-table-column label="成果类型" width="110">
              <template #default="{ row }">
                <el-tag>{{ ACH_TYPE_MAP[row.achType] || row.achType || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ruleName" label="规则名称" min-width="200" show-overflow-tooltip />
            <el-table-column label="基础分值" width="100" align="right">
              <template #default="{ row }">{{ row.baseScore ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="计算方式" width="110">
              <template #default="{ row }">{{ calcModeLabel(row.calcMode) }}</template>
            </el-table-column>
            <el-table-column prop="configJson" label="配置" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ row.configJson || '-' }}</template>
            </el-table-column>
            <el-table-column label="一票否决" width="90">
              <template #default="{ row }">
                <el-tag v-if="isFlag(row.vetoFlag)" type="danger" size="small">是</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="需整改" width="80">
              <template #default="{ row }">
                <el-tag v-if="isFlag(row.needReform)" type="warning" size="small">是</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openRuleEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="handleRuleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无业绩规则" />
            </template>
          </el-table>
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="ruleQuery.pageNum"
              v-model:page-size="ruleQuery.pageSize"
              :total="ruleTotal"
              layout="total, prev, pager, next"
              @current-change="loadRules"
            />
          </div>
        </el-tab-pane>

        <!-- 扣分规则 -->
        <el-tab-pane label="扣分规则" name="DEDUCT">
          <div class="tab-toolbar">
            <el-button type="danger" plain @click="openRuleCreate('DEDUCT')">新增扣分规则</el-button>
          </div>
          <el-table v-loading="ruleLoading" :data="ruleList" stripe>
            <el-table-column prop="ruleNo" label="规则编号" width="120">
              <template #default="{ row }">{{ row.ruleNo || '-' }}</template>
            </el-table-column>
            <el-table-column prop="ruleName" label="规则名称" min-width="220" show-overflow-tooltip />
            <el-table-column label="扣分值" width="100" align="right">
              <template #default="{ row }">{{ row.baseScore ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="计算方式" width="110">
              <template #default="{ row }">{{ calcModeLabel(row.calcMode) }}</template>
            </el-table-column>
            <el-table-column prop="configJson" label="配置" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.configJson || '-' }}</template>
            </el-table-column>
            <el-table-column label="一票否决" width="90">
              <template #default="{ row }">
                <el-tag v-if="isFlag(row.vetoFlag)" type="danger" size="small">是</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="需整改" width="80">
              <template #default="{ row }">
                <el-tag v-if="isFlag(row.needReform)" type="warning" size="small">是</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openRuleEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="handleRuleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无扣分规则" />
            </template>
          </el-table>
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="ruleQuery.pageNum"
              v-model:page-size="ruleQuery.pageSize"
              :total="ruleTotal"
              layout="total, prev, pager, next"
              @current-change="loadRules"
            />
          </div>
        </el-tab-pane>

        <!-- 系数 -->
        <el-tab-pane label="系数配置" name="COEFF">
          <el-table v-loading="coeffLoading" :data="coeffList" stripe>
            <el-table-column label="成果类型" width="130">
              <template #default="{ row }">
                <el-tag>{{ ACH_TYPE_MAP[row.achType] || row.achType || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="系数名称" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ row.coeffName || row.name || '-' }}</template>
            </el-table-column>
            <el-table-column label="系数值" width="120" align="right">
              <template #default="{ row }">
                <strong>{{ row.coeffValue ?? row.coeff ?? '-' }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="说明" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无系数配置" />
            </template>
          </el-table>
        </el-tab-pane>

        <!-- 等级阈值 -->
        <el-tab-pane label="等级阈值" name="LEVEL">
          <el-table v-loading="levelLoading" :data="levelList" stripe>
            <el-table-column label="等级" width="100">
              <template #default="{ row }">
                <el-tag :type="levelTag(row.level)">{{ levelLabel(row.level) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="等级名称" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ row.levelName || '-' }}</template>
            </el-table-column>
            <el-table-column label="分数下限" width="120" align="right">
              <template #default="{ row }">{{ row.minScore ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="分数上限" width="120" align="right">
              <template #default="{ row }">{{ row.maxScore ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="说明" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无等级阈值" />
            </template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 规则新增/编辑弹窗 -->
    <el-dialog v-model="ruleFormVisible" :title="ruleEditingId ? '编辑规则' : '新增规则'" width="560px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="ruleFormRef" :model="ruleForm" :rules="ruleFormRules" label-width="100px">
        <el-form-item label="规则类型" prop="ruleType">
          <el-radio-group v-model="ruleForm.ruleType" disabled>
            <el-radio value="ACH">业绩规则</el-radio>
            <el-radio value="DEDUCT">扣分规则</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="规则编号" prop="ruleNo">
          <el-input v-model="ruleForm.ruleNo" placeholder="如：ACH-PAPER-001" maxlength="50" />
        </el-form-item>
        <el-form-item v-if="ruleForm.ruleType === 'ACH'" label="成果类型" prop="achType">
          <el-select v-model="ruleForm.achType" placeholder="选择成果类型" style="width: 100%">
            <el-option v-for="(label, key) in ACH_TYPE_MAP" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="ruleForm.ruleName" placeholder="规则名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="基础分值" prop="baseScore">
          <el-input-number v-model="ruleForm.baseScore" :min="0" :precision="2" style="width: 100%" placeholder="基础分值" />
        </el-form-item>
        <el-form-item label="计算方式" prop="calcMode">
          <el-select v-model="ruleForm.calcMode" placeholder="选择计算方式" style="width: 100%">
            <el-option label="固定分值" value="FIXED" />
            <el-option label="系数计算" value="COEFFICIENT" />
            <el-option label="公式计算" value="FORMULA" />
          </el-select>
        </el-form-item>
        <el-form-item label="配置JSON">
          <el-input v-model="ruleForm.configJson" type="textarea" :rows="3" placeholder='如：{"rankFactor": {"第一作者": 1.0}}' maxlength="500" />
        </el-form-item>
        <el-form-item label="一票否决">
          <el-switch v-model="ruleForm.vetoFlag" />
        </el-form-item>
        <el-form-item label="需整改">
          <el-switch v-model="ruleForm.needReform" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleRuleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { rulePage, saveRule, deleteRule, coeffList as fetchCoeffList, levelList as fetchLevelList } from '@/api/system'

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

const CALC_MODE_MAP: Record<string, string> = {
  FIXED: '固定分值',
  COEFFICIENT: '系数计算',
  FORMULA: '公式计算',
}

function calcModeLabel(mode: unknown): string {
  return CALC_MODE_MAP[String(mode)] || String(mode ?? '-')
}

function isFlag(flag: unknown): boolean {
  return flag === true || flag === 1 || String(flag).toUpperCase() === 'Y' || String(flag) === '1'
}

const LEVEL_LABEL_MAP: Record<string, { label: string; type: string }> = {
  A: { label: 'A级 · 诚信优秀', type: 'success' },
  B: { label: 'B级 · 诚信合格', type: 'primary' },
  C: { label: 'C级 · 诚信警示', type: 'warning' },
  D: { label: 'D级 · 严重失信', type: 'danger' },
}
function levelLabel(level: unknown): string {
  return LEVEL_LABEL_MAP[String(level)]?.label || String(level ?? '-')
}
function levelTag(level: unknown): string {
  return LEVEL_LABEL_MAP[String(level)]?.type || 'info'
}

const activeTab = ref('ACH')

/** 规则列表 */
interface RuleQuery {
  pageNum: number
  pageSize: number
  ruleType: string
  achType?: string
}
const ruleQuery = reactive<RuleQuery>({ pageNum: 1, pageSize: 10, ruleType: 'ACH', achType: undefined })
const ruleList = ref<any[]>([])
const ruleTotal = ref(0)
const ruleLoading = ref(false)

async function loadRules() {
  ruleLoading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: ruleQuery.pageNum,
      pageSize: ruleQuery.pageSize,
      ruleType: ruleQuery.ruleType,
    }
    if (ruleQuery.achType) params.achType = ruleQuery.achType
    const res: any = await rulePage(params)
    ruleList.value = Array.isArray(res) ? res : res?.list ?? []
    ruleTotal.value = res?.total ?? ruleList.value.length
  } finally {
    ruleLoading.value = false
  }
}

function handleRuleSearch() {
  ruleQuery.pageNum = 1
  loadRules()
}

function onTabChange() {
  ruleQuery.ruleType = activeTab.value
  ruleQuery.achType = undefined
  ruleQuery.pageNum = 1
  if (activeTab.value === 'ACH' || activeTab.value === 'DEDUCT') {
    loadRules()
  } else if (activeTab.value === 'COEFF') {
    loadCoeffs()
  } else {
    loadLevels()
  }
}

/** 规则新增/编辑 */
const ruleFormVisible = ref(false)
const ruleEditingId = ref<number | null>(null)
const saving = ref(false)
const ruleFormRef = ref<FormInstance>()

interface RuleForm {
  ruleType: string
  ruleNo: string
  achType: string
  ruleName: string
  baseScore?: number
  calcMode: string
  configJson: string
  vetoFlag: boolean
  needReform: boolean
}
const ruleForm = reactive<RuleForm>({
  ruleType: 'ACH',
  ruleNo: '',
  achType: '',
  ruleName: '',
  baseScore: undefined,
  calcMode: 'FIXED',
  configJson: '',
  vetoFlag: false,
  needReform: false,
})

const ruleFormRules = reactive<FormRules>({
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  ruleNo: [{ required: true, message: '请输入规则编号', trigger: 'blur' }],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  baseScore: [{ required: true, message: '请输入基础分值', trigger: 'blur' }],
})

function openRuleCreate(type: string) {
  ruleEditingId.value = null
  Object.assign(ruleForm, {
    ruleType: type,
    ruleNo: '',
    achType: type === 'ACH' ? '' : '',
    ruleName: '',
    baseScore: undefined,
    calcMode: 'FIXED',
    configJson: '',
    vetoFlag: false,
    needReform: false,
  })
  ruleFormVisible.value = true
}

function openRuleEdit(row: any) {
  ruleEditingId.value = row.ruleId ?? row.id
  ruleForm.ruleType = row.ruleType || ruleQuery.ruleType
  ruleForm.ruleNo = row.ruleNo || ''
  ruleForm.achType = row.achType || ''
  ruleForm.ruleName = row.ruleName || ''
  ruleForm.baseScore = row.baseScore ?? undefined
  ruleForm.calcMode = row.calcMode || 'FIXED'
  ruleForm.configJson = row.configJson || ''
  ruleForm.vetoFlag = isFlag(row.vetoFlag)
  ruleForm.needReform = isFlag(row.needReform)
  ruleFormVisible.value = true
}

async function handleRuleSave() {
  try {
    await ruleFormRef.value?.validate()
  } catch {
    return
  }
  const payload: Record<string, any> = {
    ruleType: ruleForm.ruleType,
    ruleNo: ruleForm.ruleNo,
    ruleName: ruleForm.ruleName,
    baseScore: ruleForm.baseScore,
    calcMode: ruleForm.calcMode,
    configJson: ruleForm.configJson,
    vetoFlag: ruleForm.vetoFlag,
    needReform: ruleForm.needReform,
  }
  if (ruleForm.ruleType === 'ACH') payload.achType = ruleForm.achType
  if (ruleEditingId.value) payload.ruleId = ruleEditingId.value
  saving.value = true
  try {
    await saveRule(payload)
    ElMessage.success('保存成功')
    ruleFormVisible.value = false
    loadRules()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleRuleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除规则「${row.ruleName}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteRule(row.ruleId ?? row.id)
    ElMessage.success('删除成功')
    loadRules()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

/** 系数配置（展示） */
const coeffList = ref<any[]>([])
const coeffLoading = ref(false)
async function loadCoeffs() {
  coeffLoading.value = true
  try {
    const res: any = await fetchCoeffList()
    coeffList.value = Array.isArray(res) ? res : res?.list ?? []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载系数失败')
  } finally {
    coeffLoading.value = false
  }
}

/** 等级阈值（展示） */
const levelList = ref<any[]>([])
const levelLoading = ref(false)
async function loadLevels() {
  levelLoading.value = true
  try {
    const res: any = await fetchLevelList()
    levelList.value = Array.isArray(res) ? res : res?.list ?? []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载等级阈值失败')
  } finally {
    levelLoading.value = false
  }
}

onMounted(loadRules)
</script>

<style scoped>
.page-header {
  margin-bottom: 8px;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
