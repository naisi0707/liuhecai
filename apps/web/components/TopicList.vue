<script setup lang="ts">
import { DEFAULT_TOPIC_TAG, topicTagColor, type TopicVO } from '@liuhecai/shared'

defineProps<{ topics: TopicVO[] }>()

function authorOf(t: TopicVO) {
  const m = t.title.match(/【([^】]+)】/)
  return m?.[1] || t.playType || ''
}

function tagLabel(t: TopicVO) {
  return t.purchased ? '已购' : (t.tag || DEFAULT_TOPIC_TAG)
}

function tagStyle(t: TopicVO) {
  if (t.purchased) {
    return { background: '#6b7280', color: '#fff' }
  }
  const bg = topicTagColor(t.tag)
  return { background: bg, color: '#fff' }
}
</script>

<template>
  <table class="topic-table">
    <tbody>
      <tr v-for="t in topics" :key="t.id">
        <td>
          <span class="topic-tag topic-badge" :style="tagStyle(t)">{{ tagLabel(t) }}</span>
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
