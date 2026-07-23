<script setup lang="ts">
const { siteName, domainBadge, tenant } = useTenant()
const { mediaUrl } = useMediaUrl()

const src = computed(() => {
  try {
    const theme = tenant.value?.themeJson ? JSON.parse(tenant.value.themeJson) : {}
    const url = theme.bannerUrl || tenant.value?.logoUrl
    return url ? mediaUrl(url) : ''
  } catch {
    return ''
  }
})

const alt = computed(() => siteName.value || domainBadge.value || '')
</script>

<template>
  <div v-if="src" class="hero-banner">
    <img :src="src" :alt="alt" />
  </div>
</template>
