<script setup lang="ts">
withDefaults(defineProps<{
  hint?: string
  empty?: boolean
  emptyText?: string
}>(), {
  hint: '随左侧实时更新，保存后才对访客生效',
  empty: false,
  emptyText: '填写左侧内容后，这里会显示前台效果',
})
</script>

<template>
  <div class="cms-preview">
    <div class="cms-preview__bar">
      <span>前台预览</span>
      <span class="cms-preview__hint">{{ hint }}</span>
    </div>
    <div class="cms-preview__stage">
      <slot v-if="!empty" />
      <p v-else class="cms-preview__empty">{{ emptyText }}</p>
    </div>
  </div>
</template>

<style scoped>
.cms-preview {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  /* 不限制高度、不内部滚动：内容完整展开，随主区域一起滚 */
}

.cms-preview__bar {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px 12px;
  padding: 10px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  font-weight: 600;
  font-size: 14px;
}

.cms-preview__hint {
  font-weight: 400;
  font-size: 12px;
  color: #6b7280;
}

.cms-preview__stage {
  min-height: 120px;
  padding: 8px;
  background: #f3f4f6;
  overflow: visible;
}

.cms-preview__empty {
  margin: 48px 16px;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
}
</style>
