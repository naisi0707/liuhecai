<script setup lang="ts">
import { getTenantHostOverride } from '@liuhecai/shared'

const { loadTenant, errorMsg, primaryColor, siteName, tenant } = useTenant()
const { hydrateFromStorage, token, refreshProfile } = useAuth()
const { loadDraws, startCountdown, stopCountdown } = useDraws()
const { loadMenus } = useSiteCms()

const isEntry = computed(() => tenant.value?.domainRole === 'ENTRY')

const themeStyle = computed(() => (
  primaryColor.value ? { '--brand': primaryColor.value } : undefined
))

/** ENTRY 伪装入口：标签页显示「导航」，勿暴露论坛名 */
useHead(() => ({
  title: isEntry.value ? '导航' : (siteName.value || '论坛'),
}))

try {
  await loadTenant()
  if (!isEntry.value) {
    await Promise.all([loadDraws(), loadMenus()])
  }
} catch {
  // 接口失败已由 request 钩子 markSiteBusy
}

onMounted(async () => {
  hydrateFromStorage()
  const params = new URLSearchParams(window.location.search)
  const qHost = (params.get('host') || '').trim().toLowerCase()
  const expectedHost = qHost || getTenantHostOverride() || ''
  try {
    // ?host= 切换时必须重拉租户（含 domainRole）
    if (expectedHost && tenant.value?.host !== expectedHost) {
      await loadTenant(true)
    }
    if (!isEntry.value) {
      await loadDraws()
      await loadMenus()
      if (token.value) await refreshProfile()
      startCountdown()
    }
  } catch {
    // busy 已标记
  }
})

onBeforeUnmount(() => stopCountdown())

function scrollTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<template>
  <!-- ENTRY：伪装壳全屏，不套论坛顶栏 -->
  <div v-if="isEntry">
    <slot />
  </div>
  <div v-else :style="themeStyle">
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
