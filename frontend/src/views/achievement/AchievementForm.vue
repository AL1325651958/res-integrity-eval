<template>
  <el-dialog
    :model-value="modelValue"
    :title="achievementId ? '编辑成果' : '新增成果'"
    width="640px"
    destroy-on-close
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="成果类型" prop="achType">
        <el-select
          v-model="form.achType"
          placeholder="请选择成果类型"
          style="width: 100%"
          :disabled="!!achievementId || !!achTypePreset"
          @change="onTypeChange"
        >
          <el-option v-for="(label, key) in ACH_TYPE_MAP" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>

      <el-form-item label="成果标题" prop="title">
        <el-input v-model="form.title" :placeholder="titlePlaceholder" maxlength="200" show-word-limit />
      </el-form-item>

      <el-form-item v-if="meta && meta.hasSourceName" :label="meta.sourceNameLabel" prop="sourceName">
        <el-input v-model="form.sourceName" :placeholder="`请输入${meta.sourceNameLabel}`" maxlength="100" />
      </el-form-item>

      <el-form-item v-if="meta && meta.hasAchNo" :label="meta.achNoLabel" prop="achNo">
        <el-input v-model="form.achNo" :placeholder="`请输入${meta.achNoLabel}`" maxlength="100" />
      </el-form-item>

      <el-form-item v-if="meta && meta.hasLevel" label="级别/分区" prop="level">
        <el-select v-model="form.level" placeholder="请选择级别" style="width: 100%">
          <el-option v-for="opt in meta.levelOptions" :key="opt" :label="opt" :value="opt" />
        </el-select>
      </el-form-item>

      <el-form-item v-if="meta && meta.hasRank" label="作者/角色排名" prop="rankInfo">
        <el-select v-model="form.rankInfo" placeholder="请选择排名/角色" style="width: 100%">
          <el-option v-for="opt in meta.rankOptions" :key="opt" :label="opt" :value="opt" />
        </el-select>
      </el-form-item>

      <el-form-item v-if="meta && meta.hasCorresponding" label="是否通讯/一作" prop="isCorresponding">
        <el-radio-group v-model="form.isCorresponding">
          <el-radio :value="true">是</el-radio>
          <el-radio :value="false">否</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="meta && meta.hasFund" :label="meta.fundLabel" prop="fundAmount">
        <el-input-number v-model="form.fundAmount" :min="0" :precision="2" :step="1" style="width: 100%" placeholder="请输入金额" />
      </el-form-item>

      <el-form-item v-if="meta && meta.hasTime" :label="meta.timeLabel" prop="publishTime">
        <el-date-picker v-model="form.publishTime" type="date" value-format="YYYY-MM-DD" :placeholder="`请选择${meta.timeLabel}`" style="width: 100%" />
      </el-form-item>

      <el-form-item label="附件材料">
        <div class="upload-wrap">
          <el-upload
            :show-file-list="false"
            :http-request="handleUpload"
            :before-upload="beforeUpload"
            accept=".pdf,.doc,.docx,.jpg,.jpeg,.png,.xls,.xlsx,.zip,.rar"
            multiple
          >
            <el-button type="primary" plain size="small">选择文件上传</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PDF/Word/图片/Excel/压缩包，单个文件不超过 50MB（全文PDF、录用证明、检索证明等）</div>
            </template>
          </el-upload>
          <div v-if="fileList.length" class="attach-list">
            <div v-for="(f, i) in fileList" :key="f.fileId || i" class="attach-item">
              <span class="attach-icon">📎</span>
              <span class="attach-name" :title="f.fileName">{{ f.fileName }}</span>
              <el-button link type="danger" size="small" @click="removeFile(f, i)">删除</el-button>
            </div>
          </div>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadRequestOptions, UploadRawFile } from 'element-plus'
import { createAchievement, updateAchievement, getAchievement } from '@/api/achievement'
import { uploadFile, deleteFile } from '@/api/file'

const props = defineProps<{
  modelValue: boolean
  achievementId?: number | string | null
  achTypePreset?: string | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}>()

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

interface AchFieldMeta {
  achNoLabel: string
  hasSourceName: boolean
  sourceNameLabel?: string
  hasAchNo: boolean
  hasLevel: boolean
  levelOptions: string[]
  hasRank: boolean
  rankOptions: string[]
  hasFund: boolean
  fundLabel?: string
  hasCorresponding: boolean
  hasTime: boolean
  timeLabel?: string
}

/** 各成果类型动态表单字段（与接口契约第 3 节一致） */
const ACH_META: Record<string, AchFieldMeta> = {
  PAPER: {
    achNoLabel: 'DOI',
    hasSourceName: true,
    sourceNameLabel: '期刊名称',
    hasAchNo: true,
    hasLevel: true,
    levelOptions: ['SCI-1区', 'SCI-2区', 'SCI-3区', 'SCI-4区', 'EI', '中文核心', '科技核心', '一般期刊', '其他'],
    hasRank: true,
    rankOptions: ['第一作者', '通讯作者', '共同第一作者', '参与作者'],
    hasFund: false,
    hasCorresponding: true,
    hasTime: true,
    timeLabel: '发表时间',
  },
  TOPIC: {
    achNoLabel: '项目编号',
    hasSourceName: true,
    sourceNameLabel: '立项部门',
    hasAchNo: true,
    hasLevel: true,
    levelOptions: ['国家级', '省部级', '市厅级', '院级'],
    hasRank: true,
    rankOptions: ['负责人', '核心成员', '参与人'],
    hasFund: true,
    fundLabel: '项目经费(万元)',
    hasCorresponding: false,
    hasTime: true,
    timeLabel: '立项时间',
  },
  PATENT: {
    achNoLabel: '专利号',
    hasSourceName: false,
    hasAchNo: true,
    hasLevel: true,
    levelOptions: ['发明专利', '实用新型', '软件著作权', '外观设计'],
    hasRank: true,
    rankOptions: ['第1发明人', '第2发明人', '第3发明人', '其他发明人'],
    hasFund: false,
    hasCorresponding: false,
    hasTime: true,
    timeLabel: '授权时间',
  },
  REWARD: {
    achNoLabel: '奖项编号',
    hasSourceName: true,
    sourceNameLabel: '颁奖单位',
    hasAchNo: false,
    hasLevel: true,
    levelOptions: ['国家级一等奖', '国家级二等奖', '国家级三等奖', '省部级一等奖', '省部级二等奖', '省部级三等奖', '市厅级一等奖', '市厅级二等奖', '市厅级三等奖', '院级', '其他'],
    hasRank: true,
    rankOptions: ['第1名', '第2名', '第3名', '其他'],
    hasFund: false,
    hasCorresponding: false,
    hasTime: true,
    timeLabel: '获奖时间',
  },
  BOOK: {
    achNoLabel: 'ISBN',
    hasSourceName: false,
    hasAchNo: true,
    hasLevel: true,
    levelOptions: ['国家级规划教材', '普通教材', '专著'],
    hasRank: true,
    rankOptions: ['主编', '副主编', '编委'],
    hasFund: false,
    hasCorresponding: false,
    hasTime: false,
  },
  STANDARD: {
    achNoLabel: '标准号',
    hasSourceName: false,
    hasAchNo: true,
    hasLevel: true,
    levelOptions: ['国际标准', '国家标准', '行业标准', '地方标准', '团体标准', '指南共识'],
    hasRank: true,
    rankOptions: ['主要起草人', '参与起草'],
    hasFund: false,
    hasCorresponding: false,
    hasTime: false,
  },
  POST: {
    achNoLabel: '任职编号',
    hasSourceName: true,
    sourceNameLabel: '学会名称',
    hasAchNo: false,
    hasLevel: true,
    levelOptions: ['国家级', '省级', '市级'],
    hasRank: true,
    rankOptions: ['主任委员', '副主任委员', '常务委员', '委员', '理事'],
    hasFund: false,
    hasCorresponding: false,
    hasTime: false,
  },
  TRANSFER: {
    achNoLabel: '合同号',
    hasSourceName: false,
    hasAchNo: true,
    hasLevel: false,
    levelOptions: [],
    hasRank: false,
    rankOptions: [],
    hasFund: true,
    fundLabel: '到账金额(万元)',
    hasCorresponding: false,
    hasTime: true,
    timeLabel: '到账时间',
  },
}

interface FormState {
  achType: string
  title: string
  sourceName: string
  achNo: string
  level: string
  rankInfo: string
  isCorresponding: boolean
  fundAmount?: number
  publishTime: string
}

const formRef = ref<FormInstance>()
const form = reactive<FormState>({
  achType: '',
  title: '',
  sourceName: '',
  achNo: '',
  level: '',
  rankInfo: '',
  isCorresponding: false,
  fundAmount: undefined,
  publishTime: '',
})

const meta = computed<AchFieldMeta | null>(() => (form.achType ? ACH_META[form.achType] || null : null))

const titlePlaceholder = computed(() => {
  if (form.achType === 'POST') return '请输入任职名称'
  if (form.achType === 'TRANSFER') return '请输入成果转化/技术转移项目名称'
  return '请输入成果标题'
})

const rules = reactive<FormRules>({
  achType: [{ required: true, message: '请选择成果类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入成果标题', trigger: 'blur' }],
})

function onTypeChange() {
  form.level = ''
  form.rankInfo = ''
  form.sourceName = ''
  form.achNo = ''
  form.isCorresponding = false
  form.fundAmount = undefined
  form.publishTime = ''
}

function resetForm() {
  form.achType = ''
  form.title = ''
  form.sourceName = ''
  form.achNo = ''
  form.level = ''
  form.rankInfo = ''
  form.isCorresponding = false
  form.fundAmount = undefined
  form.publishTime = ''
  fileList.value = []
  attachIds.value = []
  formRef.value?.clearValidate()
}

/** 附件：选择文件即上传，记录 fileId 数组，保存时随表单提交 */
interface AttachItem {
  fileId: string
  fileName: string
}
const attachIds = ref<string[]>([])
const fileList = ref<AttachItem[]>([])
const saving = ref(false)

function beforeUpload(file: UploadRawFile): boolean {
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.warning('单个文件不能超过 50MB')
    return false
  }
  return true
}

async function handleUpload(options: UploadRequestOptions) {
  const fd = new FormData()
  fd.append('file', options.file)
  fd.append('bizType', 'ACH')
  if (props.achievementId) fd.append('bizId', String(props.achievementId))
  try {
    const res: any = await uploadFile(fd)
    const data = res?.data ?? res
    const fileId = data?.fileId
    if (!fileId) {
      ElMessage.error('上传失败：未返回文件标识')
      return
    }
    attachIds.value.push(String(fileId))
    fileList.value.push({ fileId: String(fileId), fileName: data.fileName || options.file.name })
    ElMessage.success('上传成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '上传失败')
  }
}

async function removeFile(f: AttachItem, index: number) {
  fileList.value.splice(index, 1)
  const idx = attachIds.value.indexOf(f.fileId)
  if (idx > -1) attachIds.value.splice(idx, 1)
  try {
    await deleteFile(f.fileId)
  } catch {
    /* 服务端删除失败不影响本地移除 */
  }
}

/** 编辑时回填 */
async function loadDetail(id: number | string) {
  try {
    const res: any = await getAchievement(id)
    if (!res) return
    form.achType = res.achType || props.achTypePreset || ''
    form.title = res.title || ''
    form.sourceName = res.sourceName || ''
    form.achNo = res.achNo || ''
    form.level = res.level || ''
    form.rankInfo = res.rankInfo || ''
    form.isCorresponding = !!res.isCorresponding
    form.fundAmount = res.fundAmount ?? undefined
    form.publishTime = res.publishTime || ''
    if (Array.isArray(res.attachments)) {
      fileList.value = res.attachments.map((a: any) => ({ fileId: String(a.fileId ?? a.id), fileName: a.fileName || '附件' }))
      attachIds.value = fileList.value.map((a) => a.fileId)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载成果信息失败')
  }
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      resetForm()
      if (props.achTypePreset) form.achType = props.achTypePreset
      if (props.achievementId) loadDetail(props.achievementId)
    }
  },
)

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const m = meta.value
  const payload: Record<string, any> = {
    achType: form.achType,
    title: form.title,
    attachIds: attachIds.value,
  }
  if (m) {
    if (m.hasSourceName) payload.sourceName = form.sourceName
    if (m.hasAchNo) payload.achNo = form.achNo
    if (m.hasLevel) payload.level = form.level
    if (m.hasRank) payload.rankInfo = form.rankInfo
    if (m.hasFund && form.fundAmount !== undefined && form.fundAmount !== null) payload.fundAmount = form.fundAmount
    if (m.hasCorresponding) payload.isCorresponding = form.isCorresponding
    if (m.hasTime && form.publishTime) payload.publishTime = form.publishTime
  }
  saving.value = true
  try {
    if (props.achievementId) {
      await updateAchievement(props.achievementId, payload)
    } else {
      await createAchievement(payload)
    }
    ElMessage.success('保存成功')
    emit('success')
    emit('update:modelValue', false)
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.upload-wrap {
  width: 100%;
}
.attach-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.attach-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f5f7fa;
  border-radius: 4px;
}
.attach-icon {
  color: #409eff;
  font-size: 16px;
}
.attach-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #303133;
}
</style>
