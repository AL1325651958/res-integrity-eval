<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">日志审计</span>
      </div>

      <el-tabs v-model="activeType" @tab-change="onTabChange">
        <el-tab-pane label="操作日志" name="OPERATION" />
        <el-tab-pane label="登录日志" name="LOGIN" />
      </el-tabs>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="操作人" width="130">
          <template #default="{ row }">{{ row.operatorName || row.username || row.userName || '-' }}</template>
        </el-table-column>
        <el-table-column label="模块" width="140">
          <template #default="{ row }">{{ row.module || row.bizType || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作内容" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content || row.operation || row.title || row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="IP 地址" width="150">
          <template #default="{ row }">{{ row.ip || row.ipAddr || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作时间" width="180">
          <template #default="{ row }">{{ row.operateTime || row.createTime || row.logTime || '-' }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无日志数据" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { logPage } from '@/api/system'

const activeType = ref('OPERATION')

interface Query {
  pageNum: number
  pageSize: number
  type: string
}
const query = reactive<Query>({ pageNum: 1, pageSize: 10, type: 'OPERATION' })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      type: query.type,
    }
    const res: any = await logPage(params)
    list.value = Array.isArray(res) ? res : res?.list ?? []
    total.value = res?.total ?? list.value.length
  } catch (e: any) {
    ElMessage.error(e?.message || '加载日志失败')
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  query.type = activeType.value
  query.pageNum = 1
  loadList()
}

onMounted(loadList)
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
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
