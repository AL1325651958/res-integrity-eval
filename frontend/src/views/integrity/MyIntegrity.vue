<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">我的诚信档案</span>
        <div class="header-right">
          <el-select v-model="year" style="width: 140px" @change="loadAll">
            <el-option v-for="y in years" :key="y" :label="`${y} 年度`" :value="y" />
          </el-select>
          <el-button type="primary" :loading="exporting" @click="handleExportPdf">导出 PDF</el-button>
        </div>
      </div>

      <div v-loading="loading" class="score-cards">
        <div class="score-card">
          <div class="score-label">业绩分</div>
          <div class="score-value perf">{{ formatScore(record?.perfScore) }}</div>
        </div>
        <div class="score-card">
          <div class="score-label">诚信扣分</div>
          <div class="score-value deduct">{{ formatScore(record?.deductScore) }}</div>
        </div>
        <div class="score-card">
          <div class="score-label">总分</div>
          <div class="score-value total">{{ formatScore(record?.totalScore) }}</div>
        </div>
        <div class="score-card">
          <div class="score-label">诚信等级</div>
          <div class="score-value">
            <el-tag v-if="record" :type="levelType(record.level)" size="large">{{ levelLabel(record.level) }}</el-tag>
            <span v-else>-</span>
          </div>
        </div>
      </div>

      <div v-if="record && record.vetoFlag" class="veto-tip">
        <el-tag type="danger" size="large">一票否决：本年度存在严重失信行为，评价不合格</el-tag>
      </div>

      <div class="section-title">年度评分明细</div>
      <el-table v-loading="detailLoading" :data="detailList" stripe>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="isDeduct(row.bizType) ? 'danger' : 'success'" size="small">
              {{ isDeduct(row.bizType) ? '扣分' : '业绩' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="itemName" label="评分项目" min-width="220" show-overflow-tooltip />
        <el-table-column prop="baseScore" label="基础分值" width="110" align="right" />
        <el-table-column prop="coefficient" label="系数" width="100" align="right">
          <template #default="{ row }">{{ row.coefficient ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="得分" width="120" align="right">
          <template #default="{ row }">
            <span :class="isDeduct(row.bizType) ? 'score-deduct' : 'score-perf'">
              {{ isDeduct(row.bizType) ? '-' : '' }}{{ row.score ?? '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="ruleVersion" label="规则版本" width="120">
          <template #default="{ row }">{{ row.ruleVersion || '-' }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="本年度暂无评分明细" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { myIntegrity, myIntegrityDetail, exportPdf } from '@/api/integrity'

/** 诚信等级：A 优秀 / B 合格 / C 警示 / D 严重失信 */
const LEVEL_MAP: Record<string, { label: string; type: string }> = {
  A: { label: 'A级 · 诚信优秀', type: 'success' },
  B: { label: 'B级 · 诚信合格', type: 'primary' },
  C: { label: 'C级 · 诚信警示', type: 'warning' },
  D: { label: 'D级 · 严重失信', type: 'danger' },
}

function levelLabel(level: unknown): string {
  return LEVEL_MAP[String(level)]?.label || String(level ?? '-')
}
function levelType(level: unknown): string {
  return LEVEL_MAP[String(level)]?.type || 'info'
}

/** 明细 bizType：含 DEDUCT 或中文“扣分”视为扣分项 */
function isDeduct(bizType: unknown): boolean {
  const s = String(bizType ?? '').toUpperCase()
  return s.includes('DEDUCT') || s === '扣分'
}

function formatScore(v: unknown): string {
  if (v === null || v === undefined || v === '') return '-'
  return String(v)
}

/** 年度选择：近 6 年 */
const years = ref<string[]>([])
const year = ref<string>(String(new Date().getFullYear()))

function buildYears() {
  const cur = new Date().getFullYear()
  years.value = Array.from({ length: 6 }, (_, i) => String(cur - i))
}

const record = ref<any>(null)
const detailList = ref<any[]>([])
const loading = ref(false)
const detailLoading = ref(false)
const exporting = ref(false)

async function loadSummary() {
  loading.value = true
  try {
    const res: any = await myIntegrity(year.value)
    record.value = res ?? null
  } catch (e: any) {
    ElMessage.error(e?.message || '获取年度评价失败')
  } finally {
    loading.value = false
  }
}

async function loadDetail() {
  detailLoading.value = true
  try {
    const res: any = await myIntegrityDetail(year.value)
    detailList.value = Array.isArray(res) ? res : res?.list ?? []
  } catch (e: any) {
    detailList.value = []
    ElMessage.error(e?.message || '获取评分明细失败')
  } finally {
    detailLoading.value = false
  }
}

function loadAll() {
  loadSummary()
  loadDetail()
}

async function handleExportPdf() {
  exporting.value = true
  try {
    const res: any = await exportPdf({ year: year.value })
    let blob: Blob | null = null
    if (res instanceof Blob) blob = res
    else if (res?.data instanceof Blob) blob = res.data
    if (blob) {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `科研诚信档案_${year.value}.pdf`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    } else if (typeof res === 'string') {
      window.open(res, '_blank')
    } else {
      ElMessage.warning('未获取到 PDF 文件流')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  buildYears()
  loadAll()
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.score-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.score-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}
.score-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}
.score-value {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}
.score-value.perf {
  color: #67c23a;
}
.score-value.deduct {
  color: #f56c6c;
}
.score-value.total {
  color: #409eff;
}
.veto-tip {
  margin-bottom: 16px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 20px 0 12px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
.score-perf {
  color: #67c23a;
  font-weight: 600;
}
.score-deduct {
  color: #f56c6c;
  font-weight: 600;
}
</style>
