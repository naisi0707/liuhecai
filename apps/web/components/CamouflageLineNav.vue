<script setup lang="ts">
import { encodeGotoUrl, type EntryLinePublicVO } from '@liuhecai/shared'

const { tenant } = useTenant()
const route = useRoute()

const lines = computed(() => (tenant.value?.entryLines || []).filter((l) => l.forumUrl))

function go(line: EntryLinePublicVO) {
  const u = encodeGotoUrl(line.forumUrl)
  const hostQ = typeof route.query.host === 'string' ? route.query.host : ''
  navigateTo({
    path: '/goto',
    query: hostQ ? { u, host: hostQ } : { u },
  })
}
</script>

<template>
  <div v-if="lines.length" class="line-nav">
    <div class="line-nav__wrap">
      <h1>请选择线路进入</h1>
      <button
        v-for="(line, i) in lines"
        :key="i"
        type="button"
        class="line-nav__btn"
        :style="{ background: line.color || '#c62828' }"
        @click="go(line)"
      >
        {{ line.label }}
      </button>
      <p class="line-nav__tip">
        注：如无法进入到主页，请更换其他浏览器。推荐使用 UC、手机自带、谷歌浏览器进行访问。
      </p>
    </div>
  </div>
</template>

<style scoped>
.line-nav {
  pointer-events: none;
}

.line-nav__wrap {
  pointer-events: auto;
  max-width: 420px;
  margin: 0 auto;
  padding: 12px 14px 16px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.18);
  font-family: "Microsoft YaHei", sans-serif;
  color: #111;
}

.line-nav__wrap h1 {
  margin: 0 0 10px;
  font-size: 16px;
  text-align: center;
}

.line-nav__btn {
  display: block;
  width: 100%;
  margin: 8px 0;
  padding: 10px 12px;
  text-align: center;
  font-weight: 700;
  color: #fff;
  background: #c62828;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-family: inherit;
}

.line-nav__tip {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.5;
  color: #555;
}
</style>
