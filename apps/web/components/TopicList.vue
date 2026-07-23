<script setup lang="ts">
import type { TopicVO } from '@liuhecai/shared'

defineProps<{ topics: TopicVO[] }>()

function authorOf(t: TopicVO) {
  const m = t.title.match(/【([^】]+)】/)
  return m?.[1] || t.playType || ''
}
</script>

<template>
  <table class="topic-table">
    <tbody>
      <tr v-for="t in topics" :key="t.id">
        <td>
          <span class="topic-tag topic-badge topic-badge--danger">{{ t.purchased ? '已购' : '出售帖' }}</span>
        </td>
        <td>
          <NuxtLink :to="`/topic/${t.id}`" class="topic-link">
            <span class="topic-tag">{{ t.title }}</span>
          </NuxtLink>
        </td>
        <td>
          <span v-if="authorOf(t)" class="topic-tag topic-badge topic-badge--light topic-author">
            {{ authorOf(t) }}
          </span>
        </td>
      </tr>
    </tbody>
  </table>
</template>
