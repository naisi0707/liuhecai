<script setup lang="ts">
const route = useRoute()
const { siteName, tenant, loadTenant } = useTenant()
const { token, coinBalance, logout, hydrateFromStorage, refreshProfile } = useAuth()
const { menus, loadMenus } = useSiteCms()
const { mediaUrl } = useMediaUrl()

const logoSrc = computed(() => (tenant.value?.logoUrl ? mediaUrl(tenant.value.logoUrl) : ''))

const mobileHeaderStyle = computed(() => (
  logoSrc.value
    ? {
        backgroundImage: `url(${logoSrc.value})`,
        backgroundRepeat: 'no-repeat',
        backgroundPosition: 'center',
      }
    : undefined
))

const isActive = (path: string) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const iconPath: Record<string, string> = {
  home: 'M3 10.5 12 3l9 7.5V21a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1v-10.5z',
  rules: 'M7 3h10a2 2 0 0 1 2 2v14l-3-2-3 2-3-2-3 2V5a2 2 0 0 1 2-2zm2 5h6v2H9V8zm0 4h6v2H9v-2z',
  recharge: 'M4 7a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7zm3 3h10v2H7v-2z',
  kefu: 'M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4zm0 2c-4 0-7 2-7 4v1h14v-1c0-2-3-4-7-4z',
}

function iconFor(code: string) {
  return iconPath[code] || iconPath.home
}

onMounted(async () => {
  hydrateFromStorage()
  try {
    if (!tenant.value) await loadTenant()
    if (token.value) await refreshProfile()
    await loadMenus()
  } catch {
    // 白屏
  }
})
</script>

<template>
  <header class="site-header site-header--desktop">
    <div class="site-header__inner">
      <NuxtLink v-if="logoSrc" to="/" class="site-header__brand">
        <img :src="logoSrc" :alt="siteName" height="35" />
      </NuxtLink>
      <NuxtLink v-else to="/" class="site-header__brand">{{ siteName }}</NuxtLink>
      <ul class="site-header__nav">
        <li v-for="m in menus" :key="m.code">
          <NuxtLink
            :to="m.path"
            :class="{ 'is-active': isActive(m.path) }"
          >{{ m.title }}</NuxtLink>
        </li>
      </ul>
      <div class="site-header__actions">
        <template v-if="token">
          <span>金币 {{ coinBalance ?? 0 }}</span>
          <button type="button" class="site-btn site-btn--outline-danger" @click="logout">退出</button>
        </template>
        <template v-else>
          <NuxtLink to="/register" class="site-btn site-btn--outline-danger">注册</NuxtLink>
          <NuxtLink to="/login" class="site-btn site-btn--danger">登录</NuxtLink>
        </template>
      </div>
    </div>
  </header>

  <header class="site-header site-header--mobile" :style="mobileHeaderStyle">
    <div class="site-header__mobile-bar">
      <NuxtLink to="/register">‹ 注册</NuxtLink>
      <NuxtLink to="/login">登录 ›</NuxtLink>
    </div>
  </header>

  <header class="site-header site-header--bottom">
    <ul class="site-header__tabs">
      <li v-for="m in menus" :key="'m-' + m.code">
        <NuxtLink :to="m.path" :class="{ 'is-active': isActive(m.path) }">
          <svg class="site-header__icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path :d="iconFor(m.code)" />
          </svg>
          {{ m.title }}
        </NuxtLink>
      </li>
    </ul>
  </header>
</template>
