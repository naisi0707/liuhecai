<script setup lang="ts">
const { loadTenant, errorMsg, primaryColor, siteName } = useTenant()
const { hydrateFromStorage } = useAuth()

onMounted(async () => {
  hydrateFromStorage()
  try {
    await loadTenant()
  } catch {
    // busy 已标记
  }
})
</script>

<template>
  <div class="auth-page" :style="primaryColor ? { '--brand': primaryColor } : undefined">
    <AppHeader />
    <main class="auth-page__main">
      <p v-if="errorMsg" class="global-error">{{ errorMsg }}</p>
      <slot />
    </main>
    <footer v-if="siteName" class="auth-page__foot">版权所有 不得转载 ©{{ siteName }}</footer>
  </div>
</template>
