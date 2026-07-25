<script setup lang="ts">
import { setApiBaseURL } from '@liuhecai/shared'

const config = useRuntimeConfig()
const serverBase = (config.apiBase as string) || ''
const publicBase = (config.public.apiBase as string) || ''
setApiBaseURL(import.meta.server ? (serverBase || publicBase) : publicBase)
</script>

<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>
