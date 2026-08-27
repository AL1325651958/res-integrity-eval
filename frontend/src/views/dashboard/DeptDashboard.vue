<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { deptDashboard } from '@/api/dashboard'
import { useChart } from '@/composables/useChart'
import type { DeptDashboardData, StatItem } from '@/types'

const loading = ref(false)
const dashboard = ref<DeptDashboardData>({})

const hasTypeDist = ref(false)
const hasLevelDist = ref(false)
const { el: typeChartEl, init: initTypeChart } = useChart()
const { el: levelChartEl, init: initLevelChart } = useChart()

/** 兼容 {name,value} 与 {type,count} 两种结构的图表数据 */
function toChartItems(items?: StatItem[]): { name: string; value: number }[] {
  return (items || []).map((it) => ({
    name: String(it.name || it.type || it.level || '未知'),
    value: Number(it.value ?? it.count ?? 0)
  }))
}

function renderTypePie() {
  initTypeChart({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 项' },
    legend: { bottom: 0, icon: 'circle' },
    series: [
      {
        name: '成果类型',
        type: 'pie',
        radius: ['38%', '62%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}: {c}' },
        data: toChartItems(dashboard.value.typeDist)
      }
    ]
  })
}

function renderLevelBar() {
  const items = toChartItems(dashboard.value.levelDist)
  initLevelChart({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 24, top: 24, bottom: 32 },
    xAxis: { type: 'category', data: items.map((i) => i.name), axisLabel: { interval: 0 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '成果数',
        type: 'bar',
        barMaxWidth: 36,
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        data: items.map((i) => i.value)
      }
    ]
  })
}

function riskLevelTag(level?: string): 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    高: 'danger',
    高风险: 'danger',
    中: 'warning',
    中风险: 'warning',
    低: 'info',
    低风险: 'info'
  }
  return map[level || ''] || 'info'
}

async function load() {
  loading.value = true
  try {
    const data = await deptDashboard()
    if (data) dashboard.value = data
    hasTypeDist.value = (data?.typeDist || []).length > 0
    hasLevelDist.value = (data?.levelDist || []).length > 0
    await nextTick()
    if (hasTypeDist.value) renderTypePie()
    if (hasLevelDist.value) renderLevelBar()
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
    <!-- 概览卡片 -->
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">成果总量</div>
          <div class="stat-value">{{ dashboard.achTotal ?? '--' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">未审核数量</div>
          <div class="stat-value">{{ dashboard.unAudited ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">风险人员</div>
          <div class="stat-value">{{ (dashboard.riskUsers || []).length }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="16" class="mt-16">
      <el-col :span="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span class="panel-title">成果类型分布</span>
          </template>
          <el-empty v-if="!hasTypeDist" description="暂无数据" :image-size="80" />
          <div v-else ref="typeChartEl" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span class="panel-title">成果等级分布</span>
          </template>
          <el-empty v-if="!hasLevelDist" description="暂无数据" :image-size="80" />
          <div v-else ref="levelChartEl" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 风险人员清单 -->
    <el-card shadow="never" class="panel-card mt-16">
      <template #header>
        <span class="panel-title">风险人员清单</span>
      </template>
      <el-table v-loading="loading" :data="dashboard.riskUsers || []" empty-text="暂无风险人员">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column prop="deptName" label="科室" min-width="140" />
        <el-table-column prop="riskType" label="风险类型" min-width="120" />
        <el-table-column prop="riskLevel" label="风险等级" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="riskLevelTag(row.riskLevel)" effect="light" round>
              {{ row.riskLevel || '--' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="预警时间" width="170" />
      </el-table>
    </el-card>
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
