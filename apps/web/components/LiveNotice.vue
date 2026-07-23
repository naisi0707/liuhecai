<script setup lang="ts">
const { countdownText } = useDraws()
</script>

<template>
  <div>
    <div style="color:#69f0ae;font-weight:700;">澳门六合彩 · 开奖直播提示</div>
    <div>每晚约 21:30 开奖，请刷新页面获取最新结果</div>
    <div>
      <em style="font-style:normal;color:#fff59d;margin-right:8px;">距开奖</em>
      <strong>{{ countdownText }}</strong>
    </div>
  </div>
</template>
