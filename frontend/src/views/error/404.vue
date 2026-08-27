<template>
  <div class="not-found">
    <div class="not-found-code">404</div>
    <div class="not-found-text">抱歉，您访问的页面不存在或已被移除</div>
    <el-button type="primary" @click="router.push('/')">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.not-found {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #f5f7fa;
}

.not-found-code {
  font-size: 88px;
  font-weight: 700;
  color: #c0c4cc;
  line-height: 1;
}

.not-found-text {
  font-size: 15px;
  color: #606266;
}
</style>
