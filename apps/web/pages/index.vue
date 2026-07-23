<script setup lang="ts">
import { request, type TopicVO, type CmsHomeContent } from '@liuhecai/shared'

const { authHeaders } = useAuth()
const { loadPage, pageContent } = useSiteCms()
const { mediaUrl } = useMediaUrl()

const { data: topics, refresh } = await useAsyncData('home-topics', async () => {
  try {
    return await request<TopicVO[]>('/api/topics', { headers: authHeaders() })
  } catch {
    return [] as TopicVO[]
  }
})

const banner = computed(() => {
  const home = pageContent<CmsHomeContent>('home')
  return home?.bannerUrl ? mediaUrl(home.bannerUrl) : ''
})

onMounted(async () => {
  try {
    await loadPage('home')
    if (import.meta.client && new URLSearchParams(location.search).get('host')) {
      await refresh()
    }
  } catch {
    // busy 已标记
  }
})
</script>

<template>
  <div>
    <img v-if="banner" :src="banner" width="100%" alt="banner" />
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
