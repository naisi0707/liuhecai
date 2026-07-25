<script setup lang="ts">
import type { CmsHomeContent } from '@liuhecai/shared'

const { siteName, domainBadge, tenant } = useTenant()
const { mediaUrl } = useMediaUrl()
const { loadPage, pageContent } = useSiteCms()

try {
  await loadPage('home')
} catch {
  // 无首页 CMS 时退回租户广告图
}

const src = computed(() => {
  try {
    const home = pageContent<CmsHomeContent>('home')
    const theme = tenant.value?.themeJson ? JSON.parse(tenant.value.themeJson) : {}
    // 与首页同一封面：CMS banner → 租户广告图；不用 logo
    const url = home?.bannerUrl || tenant.value?.adBanner || theme.bannerUrl || theme.adBanner
    return url ? mediaUrl(String(url)) : ''
  } catch {
    return ''
  }
})

const alt = computed(() => siteName.value || domainBadge.value || '')
</script>

<template>
  <div v-if="src" class="hero-banner">
    <img :src="src" :alt="alt" width="100%" height="auto" loading="lazy" />
  </div>
</template>
