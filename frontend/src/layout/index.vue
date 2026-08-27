<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowDown, Bell, Expand, Fold, Key, SwitchButton, User } from '@element-plus/icons-vue'
import SidebarItem from './components/SidebarItem.vue'
import { unreadCount } from '@/api/notice'
import { updatePassword } from '@/api/auth'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { findMenuPathByComponent } from '@/utils/menu'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

/** 侧边栏折叠 */
const collapsed = ref(false)

/** 当前激活菜单 */
const activeMenu = computed(() => route.path)

/** 面包屑（简单文本） */
const breadcrumbs = computed(() => {
  const titles = route.matched.filter((r) => r.meta?.title).map((r) => r.meta.title as string)
  return titles.length ? titles : ['当前页面']
})

/** 用户显示名 */
const displayName = computed(
  () => userStore.userInfo?.realName || userStore.userInfo?.username || '用户'
)
const avatarText = computed(() => displayName.value.charAt(0))

/* ---------------- 未读通知 ---------------- */
const unread = ref(0)
let unreadTimer: number | undefined

async function loadUnread() {
  try {
    unread.value = await unreadCount()
  } catch {
    // 轮询失败忽略
  }
}

function goNotice() {
  const p = findMenuPathByComponent(appStore.menus, 'notice/index')
  router.push(p || '/')
}

/* ---------------- 用户下拉 ---------------- */
async function handleCommand(command: string) {
  if (command === 'profile') {
    const p = findMenuPathByComponent(appStore.menus, 'profile/index')
    router.push(p || '/')
  } else if (command === 'password') {
    pwdVisible.value = true
  } else if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
    await userStore.logout()
    appStore.clearMenus()
    router.push('/login')
  }
}

/* ---------------- 修改密码弹窗 ---------------- */
const pwdVisible = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdSubmitting = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: string | Error) => void) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function submitPwd() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  pwdSubmitting.value = true
  try {
    await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    await userStore.logout()
    appStore.clearMenus()
    router.push('/login')
  } catch {
    // 错误提示已由请求层统一处理
  } finally {
    pwdSubmitting.value = false
  }
}

onMounted(() => {
  loadUnread()
  unreadTimer = window.setInterval(loadUnread, 60000)
})

onBeforeUnmount(() => {
  if (unreadTimer) window.clearInterval(unreadTimer)
})
</script>

<template>
  <el-container class="layout">
    <!-- 侧边栏 -->
    <el-aside :width="collapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo">
        <div class="logo-mark">诚</div>
        <span v-if="!collapsed" class="logo-text">科研诚信评价系统</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          unique-opened
          class="layout-menu"
        >
          <SidebarItem v-for="item in appStore.menus" :key="item.menuId" :item="item" />
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="layout-body">
      <!-- 顶部栏 -->
      <el-header class="layout-header" height="56px">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="18" @click="collapsed = !collapsed">
            <Expand v-if="collapsed" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
              {{ item }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 通知铃铛：未读数角标 -->
          <el-badge :value="unread" :hidden="unread === 0" :max="99" class="notice-badge">
            <el-icon class="header-icon" :size="18" @click="goNotice">
              <Bell />
            </el-icon>
          </el-badge>
          <!-- 用户下拉 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-entry">
              <el-avatar :size="30" class="user-avatar">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ displayName }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Key /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <!-- 修改密码弹窗 -->
  <el-dialog v-model="pwdVisible" title="修改密码" width="420px" :close-on-click-modal="false">
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="pwdForm.newPassword"
          type="password"
          show-password
          placeholder="请输入新密码（不少于 6 位）"
        />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="pwdForm.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入新密码"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdVisible = false">取消</el-button>
      <el-button type="primary" :loading="pwdSubmitting" @click="submitPwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.layout {
  height: 100vh;
}

.layout-aside {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #eef0f4;
  transition: width 0.2s ease;
  overflow: hidden;
}

.logo {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid #f0f2f5;

  .logo-mark {
    width: 30px;
    height: 30px;
    flex-shrink: 0;
    border-radius: 8px;
    background: linear-gradient(135deg, #2f7df6, #5aa7ff);
    color: #fff;
    font-size: 16px;
    font-weight: 600;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .logo-text {
    font-size: 15px;
    font-weight: 600;
    color: #1f2d3d;
    white-space: nowrap;
  }
}

.menu-scroll {
  flex: 1;
}

.layout-menu {
  border-right: none;
  padding: 8px;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    border-radius: 6px;
    height: 44px;
    line-height: 44px;
    margin-bottom: 2px;
  }

  :deep(.el-menu-item.is-active) {
    background: #ecf5ff;
    color: #2f7df6;
    font-weight: 500;
  }
}

.layout-body {
  min-width: 0;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #eef0f4;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #5a6473;

  &:hover {
    color: #2f7df6;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notice-badge {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.header-icon {
  color: #5a6473;

  &:hover {
    color: #2f7df6;
  }
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;

  .user-name {
    font-size: 14px;
    color: #303133;
  }

  .user-avatar {
    background: #2f7df6;
    color: #fff;
    font-size: 13px;
  }
}

.layout-main {
  background: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
