<script setup lang="ts">
const sisters = [
  { name: '至尊无上论坛', domain: '152687.xyz', cta: '查看至尊无上', href: 'https://152687.xyz' },
  { name: '神算子论坛', domain: '785412.xyz', cta: '查看神算子', href: 'https://785412.xyz' },
]
</script>

<template>
  <section class="sister-cards">
    <a
      v-for="s in sisters"
      :key="s.domain"
      class="sister-card"
      :href="s.href"
      target="_blank"
      rel="noopener noreferrer"
    >
      <div class="sister-card__name">{{ s.name }}</div>
      <div class="sister-card__domain">{{ s.domain }}</div>
      <div class="sister-card__tip">请收藏本站跟踪</div>
      <span class="sister-card__cta">{{ s.cta }}</span>
    </a>
  </section>
</template>
