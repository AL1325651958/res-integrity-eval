<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myNotices, readAllNotices, readNotice } from '@/api/notice'
import type { NoticeItem } from '@/types'

const loading = ref(false)
const list = ref<NoticeItem[]>([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })

const detailVisible = ref(false)
const detail = ref<NoticeItem | null>(null)

function isRead(row: NoticeItem): boolean {
  return row.isRead === 1 || row.isRead === true
}

async function load() {
  loading.value = true
  try {
    const res = await myNotices({ pageNum: query.pageNum, pageSize: query.pageSize })
    list.value = res?.list || []
    total.value = res?.total || 0
  } catch {
    // 错误已由请求层统一提示
  } finally {
    loading.value = false
  }
}

async function markRead(row: NoticeItem) {
  if (isRead(row)) return
  try {
    await readNotice(row.id)
    row.isRead = 1
    ElMessage.success('已标记为已读')
  } catch {
    // 忽略
  }
}

async function markAllRead() {
  try {
    await ElMessageBox.confirm('确定将所有通知标记为已读吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await readAllNotices()
  ElMessage.success('已全部标记为已读')
  load()
}

function openDetail(row: NoticeItem) {
  detail.value = row
  detailVisible.value = true
  markRead(row)
}

onMounted(load)
</script>

<template>
  <div class="page">
    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="panel-header">
          <span class="panel-title">我的通知</span>
          <el-button type="primary" plain size="small" @click="markAllRead">全部已读</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" empty-text="暂无通知" @row-click="openDetail">
        <el-table-column label="" width="50" align="center">
          <template #default="{ row }">
            <span v-if="!isRead(row)" class="unread-dot"></span>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'unread-title': !isRead(row) }">{{ row.title || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content || '--' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="!isRead(row)" type="danger" size="small" effect="light" round>未读</el-tag>
            <el-tag v-else type="info" size="small" effect="plain" round>已读</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button v-if="!isRead(row)" type="primary" link size="small" @click.stop="markRead(row)">
              标记已读
            </el-button>
            <span v-else class="read-text">已读</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <!-- 通知详情 -->
    <el-dialog v-model="detailVisible" :title="detail?.title || '通知详情'" width="560px">
      <div class="notice-detail">
        <p class="notice-time">{{ detail?.createTime }}</p>
        <p class="notice-content">{{ detail?.content }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.panel-card {
  border-radius: 8px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.unread-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
}

.unread-title {
  color: #1f2d3d;
  font-weight: 600;
}

.read-text {
  color: #c0c4cc;
  font-size: 13px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.notice-time {
  margin: 0 0 12px;
  font-size: 12px;
  color: #909399;
}

.notice-content {
  margin: 0;
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>
