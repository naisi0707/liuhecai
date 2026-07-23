<script setup lang="ts">
const { tenant } = useTenant()

const text = computed(() => tenant.value?.announcement || '')
</script>

<template>
  <div v-if="text" class="announce-bar">
    <div class="announce-bar__track">{{ text }}</div>
  </div>
</template>
