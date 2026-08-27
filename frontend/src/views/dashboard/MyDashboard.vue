<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Warning } from '@element-plus/icons-vue'
import { myDashboard } from '@/api/dashboard'
import { useAppStore } from '@/stores/app'
import type { MyDashboardData } from '@/types'
import { findMenuPathByComponent } from '@/utils/menu'

const router = useRouter()
const appStore = useAppStore()

const loading = ref(false)
const dashboard = ref<MyDashboardData>({})

const ACH_TYPE_NAMES: Record<string, string> = {
  PAPER: '论文',
  TOPIC: '课题',
  PATENT: '专利',
  REWARD: '获奖',
  BOOK: '著作',
  STANDARD: '标准',
  POST: '任职',
  TRANSFER: '转化'
}

/** 成果类型统计（取前 3 项展示） */
const achStatsList = computed(() =>
  Object.entries(dashboard.value.achStats || {}).map(([key, value]) => ({
    name: ACH_TYPE_NAMES[key] || key,
    value
  }))
)
const achStatsSummary = computed(() =>
  achStatsList.value
    .slice(0, 3)
    .map((i) => `${i.name} ${i.value}`)
    .join(' · ')
)
const achTotal = computed(() =>
  Object.values(dashboard.value.achStats || {}).reduce((sum, n) => sum + (Number(n) || 0), 0)
)

function levelTagType(level?: string): 'success' | 'primary' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'primary' | 'warning' | 'danger' | 'info'> = {
    A: 'success',
    优秀: 'success',
    B: 'primary',
    良好: 'primary',
    C: 'warning',
    合格: 'warning',
    D: 'danger',
    不合格: 'danger'
  }
  return map[level || ''] || 'info'
}

function goNotice() {
  const p = findMenuPathByComponent(appStore.menus, 'notice/index')
  router.push(p || '/')
}

async function load() {
  loading.value = true
  try {
    const data = await myDashboard()
    if (data) dashboard.value = data
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
          <div class="stat-label">年度得分</div>
          <div class="stat-value">{{ dashboard.yearScore ?? '--' }}</div>
          <div class="stat-sub">
            诚信等级
            <el-tag :type="levelTagType(dashboard.level)" effect="light" round>
              {{ dashboard.level || '--' }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">成果统计</div>
          <div class="stat-value">{{ achTotal }}</div>
          <div class="stat-sub">
            <span v-if="achStatsList.length">{{ achStatsSummary }}</span>
            <span v-else>暂无成果</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">待办提醒</div>
          <div class="stat-value">{{ (dashboard.pendingCount || 0) + (dashboard.riskCount || 0) }}</div>
          <div class="stat-sub">待办 {{ dashboard.pendingCount || 0 }} 项 · 风险预警 {{ dashboard.riskCount || 0 }} 项</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-16">
      <!-- 扣分明细 -->
      <el-col :span="16">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span class="panel-title">扣分明细</span>
          </template>
          <el-table v-loading="loading" :data="dashboard.deductList || []" empty-text="暂无扣分明细">
            <el-table-column prop="itemName" label="扣分事项" min-width="180" show-overflow-tooltip />
            <el-table-column prop="score" label="扣分" width="100" align="center">
              <template #default="{ row }">
                <span class="deduct-score">-{{ row.score }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="180" />
          </el-table>
        </el-card>
      </el-col>

      <!-- 快捷入口 -->
      <el-col :span="8">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span class="panel-title">快捷入口</span>
          </template>
          <div class="todo-list">
            <div class="todo-item" @click="goNotice">
              <el-icon class="todo-icon notice-icon"><Bell /></el-icon>
              <span class="todo-text">未读通知</span>
              <el-tag size="small" round>{{ dashboard.pendingCount || 0 }}</el-tag>
            </div>
            <div class="todo-item">
              <el-icon class="todo-icon risk-icon"><Warning /></el-icon>
              <span class="todo-text">风险预警</span>
              <el-tag size="small" type="danger" round>{{ dashboard.riskCount || 0 }}</el-tag>
            </div>
          </div>
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

.stat-sub {
  font-size: 13px;
  color: #909399;
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

.deduct-score {
  color: #f56c6c;
  font-weight: 600;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid #eef0f4;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #2f7df6;
    background: #f5f9ff;
  }
}

.todo-icon {
  font-size: 18px;
}

.notice-icon {
  color: #409eff;
}

.risk-icon {
  color: #f56c6c;
}

.todo-text {
  flex: 1;
  font-size: 14px;
  color: #303133;
}
</style>
