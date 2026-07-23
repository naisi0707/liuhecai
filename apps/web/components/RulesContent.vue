<script setup lang="ts">
import type { CmsRulesContent } from '@liuhecai/shared'

const { siteName } = useTenant()
const { loadPage, pageContent } = useSiteCms()

const content = computed(() => pageContent<CmsRulesContent>('rules'))
const heading = computed(() => content.value?.heading || '')
const intro = computed(() => content.value?.intro || '')
const guarantees = computed(() => content.value?.guarantees || [])

onMounted(async () => {
  try {
    await loadPage('rules')
  } catch {
    // busy 已标记
  }
})
</script>

<template>
  <section v-if="heading || guarantees.length" class="rules-box">
    <h3 v-if="heading" class="rules-box__title">{{ heading }}</h3>
    <div v-if="intro" class="rules-box__banner">《{{ siteName }}》{{ intro }}</div>
    <ol class="rules-box__list">
      <li v-for="(g, i) in guarantees" :key="i">
        <strong v-if="g.title">{{ g.title }}</strong>
        <SafeHtml v-if="g.body" :html="g.body" />
      </li>
    </ol>
  </section>
</template>