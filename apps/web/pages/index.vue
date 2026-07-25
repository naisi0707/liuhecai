<script setup lang="ts">
import { request, type TopicVO, type CmsHomeContent } from '@liuhecai/shared'

const { tenant } = useTenant()
const { authHeaders } = useAuth()
const { loadPage, pageContent } = useSiteCms()
const { mediaUrl } = useMediaUrl()

const isEntry = computed(() => tenant.value?.domainRole === 'ENTRY')

const { data: topics, refresh } = await useAsyncData('home-topics', async () => {
  if (isEntry.value) return [] as TopicVO[]
  try {
    const page = await request<{ records: TopicVO[] }>('/api/topics?page=1&size=50', {
      headers: authHeaders(),
    })
    return page.records || []
  } catch {
    return [] as TopicVO[]
  }
})

if (!isEntry.value) {
  try {
    await loadPage('home')
  } catch {
    // busy 已标记
  }
}

const banner = computed(() => {
  const home = pageContent<CmsHomeContent>('home')
  return home?.bannerUrl ? mediaUrl(home.bannerUrl) : ''
})

onMounted(async () => {
  if (isEntry.value) return
  try {
    if (import.meta.client && new URLSearchParams(location.search).get('host')) {
      await refresh()
    }
  } catch {
    // busy 已标记
  }
})
</script>

<template>
  <CamouflageShop v-if="isEntry" />
  <div v-else>
    <img v-if="banner" :src="banner" width="100%" height="auto" alt="banner" />
    <HomeTopBlock />
    <div class="home-guest-bar">
      您现在是游客:
      <NuxtLink to="/register">注册</NuxtLink>
      |
      <NuxtLink to="/login">登录</NuxtLink>
    </div>
    <div class="site-body">
      <TopicList :topics="topics || []" />
      <HomeBottomGallery />
    </div>
  </div>
</template>
