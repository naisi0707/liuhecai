<script setup lang="ts">
definePageMeta({ layout: 'auth' })

const name = ref('')
const password = ref('')
const { login } = useAuth()
const { errorMsg } = useTenant()
const loading = ref(false)

async function onSubmit() {
  if (!name.value.trim() || !password.value) {
    errorMsg.value = '请填写账号和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await login(name.value.trim(), password.value)
    await navigateTo('/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-stack">
    <div class="auth-card">
      <p class="auth-card__tip">只供已登记的会员使用</p>
      <form class="auth-card__form" @submit.prevent="onSubmit">
        <label>
          <span>账号:</span>
          <input v-model="name" placeholder="用户名/手机号码" autocomplete="username" />
        </label>
        <label>
          <span>密码:</span>
          <input v-model="password" type="password" placeholder="您的账户密码" autocomplete="current-password" />
        </label>
        <NuxtLink to="/register" class="auth-card__link">还没注册？&gt;</NuxtLink>
        <button type="submit" class="auth-card__btn" :disabled="loading">登 录</button>
      </form>
    </div>
  </div>
</template>
