<script setup lang="ts">
const { busy } = useSiteBusy()
</script>

<template>
  <div v-if="busy" class="site-busy">
    <p class="site-busy__text">当前网站太火爆，请稍后再试</p>
  </div>
  <NuxtLayout v-else>
    <NuxtPage />
  </NuxtLayout>
</template>
