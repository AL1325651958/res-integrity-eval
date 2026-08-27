<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">诚信评价管理</span>
        <div class="header-right">
          <el-select v-model="query.year" style="width: 140px" @change="handleSearch">
            <el-option v-for="y in years" :key="y" :label="`${y} 年度`" :value="y" />
          </el-select>
          <el-select v-model="query.level" placeholder="诚信等级" clearable style="width: 160px" @change="handleSearch">
            <el-option label="A级 · 诚信优秀" value="A" />
            <el-option label="B级 · 诚信合格" value="B" />
            <el-option label="C级 · 诚信警示" value="C" />
            <el-option label="D级 · 严重失信" value="D" />
          </el-select>
          <el-input v-model="query.keyword" placeholder="姓名/工号" clearable style="width: 180px" @keyup.enter="handleSearch" />
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button type="warning" :loading="calculating" @click="handleCalc">触发计算</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="realName" label="姓名" width="120">
          <template #default="{ row }">{{ row.realName || row.userName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="科室" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deptName || '-' }}</template>
        </el-table-column>
        <el-table-column label="业绩分" width="110" align="right">
          <template #default="{ row }">
            <span class="perf-color">{{ row.perfScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="扣分" width="100" align="right">
          <template #default="{ row }">
            <span class="deduct-color">{{ row.deductScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总分" width="110" align="right">
          <template #default="{ row }">
            <strong>{{ row.totalScore ?? '-' }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="140">
          <template #default="{ row }">
            <el-tag :type="levelType(row.level)">{{ levelLabel(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="一票否决" width="100">
          <template #default="{ row }">
            <el-tag v-if="isVeto(row.vetoFlag)" type="danger">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看明细</el-button>
            <el-button link type="warning" @click="openPublicity(row)">发起公示</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无评价数据" />
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

    <!-- 明细弹窗 -->
    <el-dialog v-model="detailVisible" :title="`评价明细 · ${detailRow?.realName || detailRow?.userName || ''}`" width="760px" destroy-on-close>
      <div v-loading="detailLoading">
        <el-descriptions :column="4" border size="small" class="detail-summary">
          <el-descriptions-item label="业绩分">{{ detailRow?.perfScore ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="扣分">{{ detailRow?.deductScore ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ detailRow?.totalScore ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="等级">
            <el-tag :type="levelType(detailRow?.level)" size="small">{{ levelLabel(detailRow?.level) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="detailTab" class="detail-tabs">
          <el-tab-pane label="业绩明细" name="perf">
            <el-table :data="perfItems" size="small">
              <el-table-column prop="itemName" label="评分项目" min-width="200" show-overflow-tooltip />
              <el-table-column prop="baseScore" label="基础分值" width="110" align="right" />
              <el-table-column prop="coefficient" label="系数" width="90" align="right">
                <template #default="{ row }">{{ row.coefficient ?? '-' }}</template>
              </el-table-column>
              <el-table-column label="得分" width="110" align="right">
                <template #default="{ row }">
                  <span class="perf-color">{{ row.score ?? '-' }}</span>
                </template>
              </el-table-column>
              <template #empty>
                <el-empty description="暂无业绩明细" :image-size="60" />
              </template>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="扣分明细" name="deduct">
            <el-table :data="deductItems" size="small">
              <el-table-column prop="itemName" label="扣分项目" min-width="200" show-overflow-tooltip />
              <el-table-column prop="baseScore" label="基础分值" width="110" align="right" />
              <el-table-column prop="coefficient" label="系数" width="90" align="right">
                <template #default="{ row }">{{ row.coefficient ?? '-' }}</template>
              </el-table-column>
              <el-table-column label="扣分" width="110" align="right">
                <template #default="{ row }">
                  <span class="deduct-color">-{{ row.score ?? '-' }}</span>
                </template>
              </el-table-column>
              <template #empty>
                <el-empty description="暂无扣分明细" :image-size="60" />
              </template>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <!-- 发起公示弹窗 -->
    <el-dialog v-model="publicityVisible" title="发起评价结果公示" width="520px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="publicityFormRef" :model="publicityForm" :rules="publicityRules" label-width="90px">
        <el-form-item label="公示对象">
          <span class="publicity-target">{{ detailRow?.realName || detailRow?.userName || '-' }}（{{ detailRow?.deptName || '-' }}）</span>
        </el-form-item>
        <el-form-item label="公示范围" prop="scope">
          <el-radio-group v-model="publicityForm.scope">
            <el-radio value="ALL">全院公示</el-radio>
            <el-radio value="DEPT">科室公示</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="公示开始" prop="startTime">
          <el-date-picker v-model="publicityForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="公示结束" prop="endTime">
          <el-date-picker v-model="publicityForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="publicityForm.remark" type="textarea" :rows="3" placeholder="公示说明（选填）" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publicityVisible = false">取消</el-button>
        <el-button type="primary" :loading="publicityLoading" @click="handlePublicity">确定发起</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { integrityPage, calcIntegrity, userIntegrityDetail, publicityIntegrity } from '@/api/integrity'

const LEVEL_MAP: Record<string, { label: string; type: string }> = {
  A: { label: 'A级 · 优秀', type: 'success' },
  B: { label: 'B级 · 合格', type: 'primary' },
  C: { label: 'C级 · 警示', type: 'warning' },
  D: { label: 'D级 · 严重失信', type: 'danger' },
}

function levelLabel(level: unknown): string {
  return LEVEL_MAP[String(level)]?.label || String(level ?? '-')
}
function levelType(level: unknown): string {
  return LEVEL_MAP[String(level)]?.type || 'info'
}

function isVeto(flag: unknown): boolean {
  return flag === true || flag === 1 || String(flag).toUpperCase() === 'Y' || String(flag) === '1'
}

const years = ref<string[]>([])
function buildYears() {
  const cur = new Date().getFullYear()
  years.value = Array.from({ length: 6 }, (_, i) => String(cur - i))
}

interface Query {
  pageNum: number
  pageSize: number
  year: string
  level?: string
  keyword?: string
}
const query = reactive<Query>({ pageNum: 1, pageSize: 10, year: String(new Date().getFullYear()), level: undefined, keyword: '' })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, any> = { pageNum: query.pageNum, pageSize: query.pageSize, year: query.year }
    if (query.level) params.level = query.level
    if (query.keyword) params.keyword = query.keyword
    const res: any = await integrityPage(params)
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

/** 触发年度评价计算 */
const calculating = ref(false)
async function handleCalc() {
  try {
    await ElMessageBox.confirm(`确认触发 ${query.year} 年度诚信评价计算？将按当前规则重新计算全员评分。`, '触发计算', {
      type: 'warning',
    })
  } catch {
    return
  }
  calculating.value = true
  try {
    await calcIntegrity(query.year)
    ElMessage.success('年度评价计算已触发')
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '触发失败')
  } finally {
    calculating.value = false
  }
}

/** 查看明细 */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRow = ref<any>(null)
const detailTab = ref('perf')
const perfItems = ref<any[]>([])
const deductItems = ref<any[]>([])

function splitItems(items: any[]) {
  perfItems.value = items.filter((it) => !isDeduct(it.bizType))
  deductItems.value = items.filter((it) => isDeduct(it.bizType))
}

function isDeduct(bizType: unknown): boolean {
  const s = String(bizType ?? '').toUpperCase()
  return s.includes('DEDUCT') || s === '扣分'
}

async function openDetail(row: any) {
  detailRow.value = row
  detailVisible.value = true
  detailLoading.value = true
  detailTab.value = 'perf'
  perfItems.value = []
  deductItems.value = []
  try {
    // 管理端按用户查看年度评价明细
    const res: any = await userIntegrityDetail(row.userId, query.year)
    const items = Array.isArray(res?.details) ? res.details : (Array.isArray(res) ? res : [])
    splitItems(items)
  } catch (e: any) {
    ElMessage.error(e?.message || '获取明细失败')
  } finally {
    detailLoading.value = false
  }
}

/** 发起公示 */
const publicityVisible = ref(false)
const publicityLoading = ref(false)
const publicityFormRef = ref<FormInstance>()
const publicityForm = reactive({
  scope: 'ALL',
  startTime: '',
  endTime: '',
  remark: '',
})
const publicityRules = reactive<FormRules>({
  scope: [{ required: true, message: '请选择公示范围', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择公示开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择公示结束时间', trigger: 'change' }],
})

function openPublicity(row: any) {
  detailRow.value = row
  publicityForm.scope = 'ALL'
  publicityForm.startTime = ''
  publicityForm.endTime = ''
  publicityForm.remark = ''
  publicityVisible.value = true
}

async function handlePublicity() {
  try {
    await publicityFormRef.value?.validate()
  } catch {
    return
  }
  publicityLoading.value = true
  try {
    await publicityIntegrity(detailRow.value.integrityId, {
      scope: publicityForm.scope,
      startTime: publicityForm.startTime,
      endTime: publicityForm.endTime,
      remark: publicityForm.remark,
    })
    ElMessage.success('公示已发起')
    publicityVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.message || '发起公示失败')
  } finally {
    publicityLoading.value = false
  }
}

onMounted(() => {
  buildYears()
  loadList()
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
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.perf-color {
  color: #67c23a;
  font-weight: 600;
}
.deduct-color {
  color: #f56c6c;
  font-weight: 600;
}
.detail-summary {
  margin-bottom: 16px;
}
.detail-tabs {
  margin-top: 8px;
}
.publicity-target {
  color: #303133;
  font-size: 13px;
}
</style>
