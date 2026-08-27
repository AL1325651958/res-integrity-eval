<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { hospitalDashboard } from '@/api/dashboard'
import { useChart } from '@/composables/useChart'
import type { HospitalDashboardData, StatItem } from '@/types'

const loading = ref(false)
const dashboard = ref<HospitalDashboardData>({
  summary: {},
  deptRank: [],
  riskTypeStat: [],
  yearTrend: [],
  seriousList: [],
  pendingChecks: 0
})

const hasRiskType = ref(false)
const hasYearTrend = ref(false)
const { el: riskChartEl, init: initRiskChart } = useChart()
const { el: trendChartEl, init: initTrendChart } = useChart()

/** 兼容 {name,value} 与 {type,count} 两种结构的图表数据 */
function toChartItems(items?: StatItem[]): { name: string; value: number }[] {
  return (items || []).map((it) => ({
    name: String(it.name || it.type || it.level || '未知'),
    value: Number(it.value ?? it.count ?? 0)
  }))
}

function renderRiskPie() {
  initRiskChart({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 件' },
    legend: { bottom: 0, icon: 'circle' },
    series: [
      {
        name: '失信问题类型',
        type: 'pie',
        radius: ['38%', '62%'],
        center: ['50%', '44%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}: {c}' },
        data: toChartItems(dashboard.value.riskTypeStat)
      }
    ]
  })
}

function renderTrendLine() {
  const items = toChartItems(dashboard.value.yearTrend)
  initTrendChart({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 24, top: 24, bottom: 32 },
    xAxis: { type: 'category', data: items.map((i) => i.name), boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '成果数',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        itemStyle: { color: '#409eff' },
        areaStyle: { opacity: 0.12 },
        data: items.map((i) => i.value)
      }
    ]
  })
}

async function load() {
  loading.value = true
  try {
    const data = await hospitalDashboard()
    if (data) dashboard.value = data
    hasRiskType.value = (data?.riskTypeStat || []).length > 0
    hasYearTrend.value = (data?.yearTrend || []).length > 0
    await nextTick()
    if (hasRiskType.value) renderRiskPie()
    if (hasYearTrend.value) renderTrendLine()
  } catch {
    // 错误已由请求层统一提示
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <!-- 总览卡片 -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">系统用户</div>
          <div class="stat-value">{{ dashboard.summary?.users ?? '--' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">成果总量</div>
          <div class="stat-value">{{ dashboard.summary?.achTotal ?? '--' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">平均诚信分</div>
          <div class="stat-value">{{ dashboard.summary?.avgScore ?? '--' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">待处置工单</div>
          <div class="stat-value stat-danger">{{ dashboard.pendingChecks ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 排名 + 类型统计 -->
    <el-row :gutter="16" class="mt-16">
      <el-col :span="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span class="panel-title">科室诚信排名</span>
          </template>
          <el-table v-loading="loading" :data="dashboard.deptRank || []" empty-text="暂无排名数据">
            <el-table-column type="index" label="排名" width="70" align="center" />
            <el-table-column prop="deptName" label="科室" min-width="140" />
            <el-table-column label="平均诚信分" min-width="110" align="center">
              <template #default="{ row }">
                {{ row.avgScore ?? row.totalScore ?? row.score ?? '--' }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span class="panel-title">失信问题类型统计</span>
          </template>
          <el-empty v-if="!hasRiskType" description="暂无数据" :image-size="80" />
          <div v-else ref="riskChartEl" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势 + 严重失信名单 -->
    <el-row :gutter="16" class="mt-16">
      <el-col :span="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span class="panel-title">年度成果趋势</span>
          </template>
          <el-empty v-if="!hasYearTrend" description="暂无数据" :image-size="80" />
          <div v-else ref="trendChartEl" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span class="panel-title">严重失信名单</span>
          </template>
          <el-table :data="dashboard.seriousList || []" empty-text="暂无严重失信人员">
            <el-table-column prop="realName" label="姓名" min-width="100" />
            <el-table-column prop="deptName" label="科室" min-width="130" />
            <el-table-column prop="level" label="诚信等级" width="110" align="center">
              <template #default="{ row }">
                <el-tag type="danger" effect="light" round>{{ row.level || '--' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-card {
  border-radius: 8px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  margin: 10px 0 8px;
  font-size: 30px;
  font-weight: 600;
  color: #303133;
  line-height: 1;
}

.stat-danger {
  color: #f56c6c;
}

.mt-16 {
  margin-top: 16px;
}

.panel-card {
  border-radius: 8px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chart {
  height: 320px;
}
</style>
