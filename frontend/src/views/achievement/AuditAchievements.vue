<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-header">
        <span class="page-title">成果审核</span>
        <div class="header-actions">
          <el-upload
            ref="uploadRef"
            :show-file-list="false"
            :auto-upload="false"
            accept=".xlsx,.xls"
            :on-change="handleImportFile"
          >
            <el-button type="primary" plain>Excel 导入</el-button>
          </el-upload>
          <el-button @click="handleTemplate">下载模板</el-button>
          <el-button type="primary" @click="handleExport">导出</el-button>
        </div>
      </div>

      <el-tabs v-model="activeScope" @tab-change="onTabChange">
        <el-tab-pane label="科室初审" name="DEPT" />
        <el-tab-pane label="科研科终审" name="ALL" />
      </el-tabs>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="title" label="成果标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="成果类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ ACH_TYPE_MAP[row.achType] || row.achType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别/分区" width="130" show-overflow-tooltip />
        <el-table-column label="提交人" width="110">
          <template #default="{ row }">{{ row.userName || row.realName || row.creatorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button link type="success" @click="openApprove(row)">通过</el-button>
            <el-button link type="danger" @click="openBack(row)">退回</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无待审成果" />
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

    <!-- 审核意见弹窗 -->
    <el-dialog v-model="auditVisible" :title="auditForm.auditType === 'APPROVE' ? '审核通过' : '审核退回'" width="480px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item label="成果标题">
          <span class="audit-title">{{ currentRow?.title }}</span>
        </el-form-item>
        <el-form-item label="审核意见" prop="opinion">
          <el-input
            v-model="auditForm.opinion"
            type="textarea"
            :rows="4"
            :placeholder="auditForm.auditType === 'APPROVE' ? '审核意见（选填）' : '请填写退回原因（必填）'"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditing" @click="handleAudit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="成果详情" size="560px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="成果标题" :span="2">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="成果类型">{{ ACH_TYPE_MAP[detail.achType] || detail.achType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交人">{{ detail.userName || detail.realName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="编号">{{ detail.achNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="来源/单位">{{ detail.sourceName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="级别/分区">{{ detail.level || '-' }}</el-descriptions-item>
            <el-descriptions-item label="作者排名">{{ detail.rankInfo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="是否通讯/一作">{{ detail.isCorresponding ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="经费(万元)">{{ detail.fundAmount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="发表/立项时间">{{ detail.publishTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">附件列表</div>
          <div v-if="detail.attachments && detail.attachments.length" class="attach-list">
            <div v-for="att in detail.attachments" :key="att.fileId" class="attach-item">
              <el-link type="primary" :underline="false" @click="downloadAtt(att)">{{ att.fileName }}</el-link>
            </div>
          </div>
          <el-empty v-else description="暂无附件" :image-size="60" />

          <div class="section-title">审核流水</div>
          <el-timeline v-if="detail.auditLogs && detail.auditLogs.length">
            <el-timeline-item v-for="(log, idx) in detail.auditLogs" :key="idx" :timestamp="log.auditTime" placement="top">
              <div class="timeline-item">
                <el-tag :type="log.auditType === 'APPROVE' ? 'success' : 'danger'" size="small">
                  {{ log.auditType === 'APPROVE' ? '通过' : '退回' }}
                </el-tag>
                <span class="timeline-name">{{ log.auditName || '审核人' }}</span>
                <div v-if="log.opinion" class="timeline-opinion">{{ log.opinion }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审核记录" :image-size="60" />
        </template>
        <el-empty v-if="!detailLoading && !detail" description="未获取到成果详情" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadInstance, UploadFile } from 'element-plus'
import { auditPage, getAchievement, auditAchievement, exportAchievements } from '@/api/achievement'
import { downloadFile } from '@/api/file'
import { importAchievements, downloadTemplate } from '@/api/importApi'

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

/** Tab：科室初审 scope=DEPT（待科室初审），科研科终审 scope=ALL（待科研科终审） */
const activeScope = ref<'DEPT' | 'ALL'>('DEPT')

interface Query {
  pageNum: number
  pageSize: number
  status?: string
  scope?: string
}
const query = reactive<Query>({ pageNum: 1, pageSize: 10, scope: 'DEPT' })
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      scope: query.scope,
      status: query.scope === 'DEPT' ? '1' : '2',
    }
    const res: any = await auditPage(params)
    list.value = Array.isArray(res) ? res : res?.list ?? []
    total.value = res?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  query.scope = activeScope.value
  query.pageNum = 1
  loadList()
}

/** 审核通过/退回 */
const auditVisible = ref(false)
const auditing = ref(false)
const currentRow = ref<any>(null)
const auditFormRef = ref<FormInstance>()
const auditForm = reactive({ auditType: 'APPROVE', opinion: '' })
const auditRules = reactive<FormRules>({
  opinion: [
    {
      validator: (_rule: any, value: string, callback: (err?: Error) => void) => {
        if (auditForm.auditType === 'BACK' && !value?.trim()) {
          callback(new Error('退回时必须填写审核意见'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
})

function openApprove(row: any) {
  currentRow.value = row
  auditForm.auditType = 'APPROVE'
  auditForm.opinion = ''
  auditVisible.value = true
}

function openBack(row: any) {
  currentRow.value = row
  auditForm.auditType = 'BACK'
  auditForm.opinion = ''
  auditVisible.value = true
}

async function handleAudit() {
  try {
    await auditFormRef.value?.validate()
  } catch {
    return
  }
  auditing.value = true
  try {
    await auditAchievement(currentRow.value.id, {
      auditType: auditForm.auditType,
      opinion: auditForm.opinion.trim(),
    })
    ElMessage.success(auditForm.auditType === 'APPROVE' ? '已通过' : '已退回')
    auditVisible.value = false
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    auditing.value = false
  }
}

/** Excel 批量导入 */
const uploadRef = ref<UploadInstance>()
const importing = ref(false)

async function handleImportFile(file: UploadFile) {
  if (importing.value) return
  if (!file.raw) return
  try {
    await ElMessageBox.confirm('导入将批量创建成果（草稿状态），请确认模板格式正确。', '导入确认', { type: 'info' })
  } catch {
    uploadRef.value?.clearFiles()
    return
  }
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.raw)
    const res: any = await importAchievements(fd)
    ElMessage.success(`导入成功${res?.count != null ? `，共 ${res.count} 条` : ''}`)
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '导入失败')
  } finally {
    importing.value = false
    uploadRef.value?.clearFiles()
  }
}

function handleTemplate() {
  downloadTemplate()
    .then((res: any) => {
      downloadBlob(res, '成果导入模板.xlsx')
    })
    .catch((e: any) => ElMessage.error(e?.message || '模板下载失败'))
}

async function handleExport() {
  try {
    const params: Record<string, any> = { scope: query.scope }
    if (query.scope === 'DEPT') params.status = '1'
    else params.status = '2'
    const res: any = await exportAchievements(params)
    downloadBlob(res, `待审成果_${new Date().toISOString().slice(0, 10)}.xlsx`)
  } catch (e: any) {
    ElMessage.error(e?.message || '导出失败')
  }
}

/** 详情抽屉 */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

async function openDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res: any = await getAchievement(row.id)
    detail.value = res ?? row
  } catch (e: any) {
    ElMessage.error(e?.message || '获取详情失败')
  } finally {
    detailLoading.value = false
  }
}

function downloadBlob(res: any, filename: string) {
  let blob: Blob | null = null
  if (res instanceof Blob) blob = res
  else if (res?.data instanceof Blob) blob = res.data
  if (blob) {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } else if (typeof res === 'string') {
    window.open(res, '_blank')
  } else {
    ElMessage.warning('未获取到文件流')
  }
}

async function downloadAtt(att: any) {
  try {
    const res: any = await downloadFile(att.fileId)
    downloadBlob(res, att.fileName || '附件')
  } catch (e: any) {
    ElMessage.error(e?.message || '下载失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.audit-title {
  color: #303133;
  font-size: 13px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 20px 0 12px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}
.attach-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.timeline-item {
  font-size: 13px;
}
.timeline-name {
  margin-left: 8px;
  color: #606266;
}
.timeline-opinion {
  margin-top: 4px;
  color: #909399;
  background: #f5f7fa;
  padding: 6px 10px;
  border-radius: 4px;
}
</style>
