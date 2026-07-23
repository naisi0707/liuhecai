<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '登录', layout: false })

const { login, hydrate } = useAdminAuth()
const isDev = import.meta.dev
const username = ref(isDev ? 'admin' : '')
const password = ref(isDev ? 'admin123' : '')
const loading = ref(false)

onMounted(() => {
  hydrate()
  // 已登录跳转由中间件处理，避免重复 navigate
})

async function onLogin() {
  loading.value = true
  try {
    await login(username.value, password.value)
    await navigateTo('/')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card style="width:420px;">
      <template #header>超管登录</template>
      <el-form label-width="72px" @submit.prevent="onLogin">
        <el-form-item label="用户名"><el-input v-model="username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="password" type="password" show-password /></el-form-item>
        <el-button type="primary" :loading="loading" @click="onLogin">登录</el-button>
      </el-form>
      <p v-if="isDev" style="color:#6b7280;margin-top:12px;">演示：admin / admin123</p>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: #f3f5f8;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 8vh;
}
</style>
