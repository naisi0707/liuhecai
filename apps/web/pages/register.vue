<script setup lang="ts">
definePageMeta({ layout: 'auth' })

const name = ref('')
const password = ref('')
const phone = ref('')
const reason = ref('')
const { register } = useAuth()
const { errorMsg } = useTenant()
const loading = ref(false)

async function onSubmit() {
  if (!name.value.trim() || !password.value) {
    errorMsg.value = '请填写帐号和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    void phone.value
    void reason.value
    await register(name.value.trim(), password.value)
    await navigateTo('/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-stack">
    <h2 class="auth-card__title">用 户 注 册</h2>
    <div class="auth-card">
    <p class="auth-card__tip">请填写真实资料，客服将联系核实</p>
    <form class="auth-card__form" @submit.prevent="onSubmit">
      <label>
        <span>帐号:</span>
        <input v-model="name" placeholder="请输入姓名(必填)" autocomplete="username" required />
      </label>
      <label>
        <span>密码:</span>
        <input v-model="password" type="password" placeholder="请输入密码(必填)" autocomplete="new-password" required minlength="6" />
      </label>
      <label>
        <span>手机:</span>
        <input v-model="phone" placeholder="您的手机号码(必填)" />
      </label>
      <label>
        <span>原因:</span>
        <input v-model="reason" placeholder="注册原因(必填)" />
      </label>
      <NuxtLink to="/login" class="auth-card__link">已注册，去登录&gt;</NuxtLink>
      <button type="submit" class="auth-card__btn" :disabled="loading">注册提交</button>
    </form>
    </div>
  </div>
</template>
