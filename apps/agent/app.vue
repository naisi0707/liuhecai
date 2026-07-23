<script setup lang="ts">
import { setApiBaseURL } from '@liuhecai/shared'

const config = useRuntimeConfig()
setApiBaseURL(config.public.apiBase as string)
</script>

<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>
