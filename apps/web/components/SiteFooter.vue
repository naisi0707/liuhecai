<script setup lang="ts">
const { siteName } = useTenant()
</script>

<template>
  <footer class="site-footer">
    <p>免责提示：未满十八岁人士、无民事行为能力人，请勿浏览本站内容，本站拒绝提供任何服务。</p>
    <p class="site-footer__copy">版权所有 不得转载 ©{{ siteName }}</p>
  </footer>
</template>
