<script setup lang="ts">
import { getTenantHostOverride } from '@liuhecai/shared'

const { loadTenant, errorMsg, primaryColor, siteName } = useTenant()
const { hydrateFromStorage, token, refreshProfile } = useAuth()
const { loadDraws, startCountdown, stopCountdown } = useDraws()

const themeStyle = computed(() => (
  primaryColor.value ? { '--brand': primaryColor.value } : undefined
))

try {
  await loadTenant()
  await loadDraws()
} catch {
  // 接口失败已由 request 钩子 markSiteBusy
}

onMounted(async () => {
  hydrateFromStorage()
  const params = new URLSearchParams(window.location.search)
  const qHost = (params.get('host') || '').trim()
  try {
    if (qHost || getTenantHostOverride()) {
      await Promise.all([loadTenant(), loadDraws()])
    }
    if (token.value) await refreshProfile()
  } catch {
    // busy 已标记
  }
  startCountdown()
})

onBeforeUnmount(() => stopCountdown())

function scrollTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<template>
  <div :style="themeStyle">
    <AppHeader />
    <div class="site-shell">
      <p v-if="errorMsg" class="global-error">{{ errorMsg }}</p>
      <div class="site-main">
        <slot />
      </div>
      <div class="site-footer">
        <p>免责提示：未满十八岁人士、无民事行为能力人，请勿浏览本站内容，本站拒绝提供任何服务。</p>
        <p v-if="siteName">版权所有 不得转载 ©{{ siteName }}</p>
      </div>
    </div>
    <div class="site-scroll-top">
      <a href="javascript:void(0)" title="返回顶部" @click.prevent="scrollTop">
        <img src="/site/images/top.png" width="60" height="60" alt="top" />
      </a>
    </div>
  </div>
</template>
