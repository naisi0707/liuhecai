<script setup lang="ts">
import type { CmsHomeContent } from '@liuhecai/shared'

const { pageContent } = useSiteCms()
const { mediaUrl } = useMediaUrl()

const images = computed(() => {
  const home = pageContent<CmsHomeContent>('home')
  const list = home?.bottomImages || []
  return list
    .filter((img) => !!img?.src)
    .map((img) => ({ ...img, src: mediaUrl(img.src) }))
})
</script>

<template>
  <div v-if="images.length" class="home-bottom-gallery">
    <img
      v-for="img in images"
      :key="img.src"
      :src="img.src"
      :alt="img.alt || ''"
      width="100%"
      height="auto"
      loading="lazy"
    />
  </div>
</template>
