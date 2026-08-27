<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { updatePassword } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const rolesText = computed(() => (userInfo.value?.roles || []).join('、') || '--')

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: string | Error) => void) => {
        if (value !== form.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await updatePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    ElMessage.success('密码修改成功')
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
  } catch {
    // 错误已由请求层统一提示
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (!userStore.userInfo) {
    userStore.fetchInfo().catch(() => {
      // 拉取失败时保留本地已有信息
    })
  }
})
</script>

<template>
  <div class="page">
    <el-card shadow="never" class="panel-card">
      <template #header>
        <span class="panel-title">个人信息</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ userInfo?.username || '--' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ userInfo?.realName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="所属科室">{{ userInfo?.deptName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="职称">{{ userInfo?.title || '--' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ rolesText }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="panel-card mt-16">
      <template #header>
        <span class="panel-title">修改密码</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="pwd-form">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="不少于 6 位" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.panel-card {
  border-radius: 8px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.mt-16 {
  margin-top: 16px;
}

.pwd-form {
  max-width: 480px;
}
</style>
