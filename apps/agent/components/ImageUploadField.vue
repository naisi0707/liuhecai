<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  label?: string
  tip?: string
}>(), {
  modelValue: '',
  label: '点击或拖拽上传图片',
  tip: '支持 jpg / png / gif / webp，最大 2MB；上传即更换',
})

const emit = defineEmits<{ 'update:modelValue': [string] }>()

const { authHeaders, hydrate } = useAgentAuth()
const config = useRuntimeConfig()
const apiBase = (config.public.apiBase as string) || ''
const webBase = (config.public.webBase as string) || 'http://127.0.0.1:3000'
const uploading = ref(false)

const localUrl = computed({
  get: () => props.modelValue || '',
  set: (v: string) => emit('update:modelValue', v),
})

function previewSrc(path: string) {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  if (path.startsWith('/uploads/')) return `${apiBase}${path}`
  if (path.startsWith('/bbs/')) return `${webBase.replace(/\/$/, '')}${path}`
  return `${apiBase}${path.startsWith('/') ? '' : '/'}${path}`
}

async function customRequest(options: UploadRequestOptions) {
  uploading.value = true
  const form = new FormData()
  form.append('file', options.file as File)
  try {
    const res = await $fetch<{ code: number; message?: string; data: { url: string } }>(
      `${apiBase}/api/agent/uploads`,
      {
        method: 'POST',
        body: form,
        headers: authHeaders(),
      },
    )
    if (res.code !== 0 || !res.data?.url) {
      throw new Error(res.message || '上传失败')
    }
    localUrl.value = res.data.url
    options.onSuccess?.(res)
    ElMessage.success('已更换图片')
  } catch (e: unknown) {
    const err = e instanceof Error ? e : new Error('上传失败')
    options.onError?.(err as any)
    ElMessage.error(err.message)
  } finally {
    uploading.value = false
  }
}

function clear(e: Event) {
  e.stopPropagation()
  e.preventDefault()
  localUrl.value = ''
}

onMounted(() => hydrate())
</script>

<template>
  <div class="img-upload-field" v-loading="uploading">
    <el-upload
      class="img-uploader"
      drag
      :show-file-list="false"
      accept="image/jpeg,image/png,image/gif,image/webp"
      :http-request="customRequest"
      :disabled="uploading"
    >
      <div v-if="localUrl" class="img-preview">
        <img :src="previewSrc(localUrl)" alt="preview" />
        <div class="img-mask">
          <span>点击更换图片</span>
          <el-button type="danger" size="small" :icon="Delete" circle @click="clear" />
        </div>
      </div>
      <div v-else class="img-empty">
        <el-icon :size="48"><Plus /></el-icon>
        <div class="img-empty__label">{{ label }}</div>
        <div class="img-empty__tip">{{ tip }}</div>
      </div>
    </el-upload>
  </div>
</template>

<style scoped>
.img-upload-field :deep(.el-upload) {
  width: 100%;
  max-width: 640px;
}
.img-upload-field :deep(.el-upload-dragger) {
  width: 100%;
  height: auto;
  min-height: 320px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  overflow: hidden;
}
.img-preview {
  position: relative;
  width: 100%;
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}
.img-preview img {
  max-width: 100%;
  max-height: 480px;
  width: auto;
  height: auto;
  object-fit: contain;
  display: block;
}
.img-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #fff;
  font-size: 15px;
  background: rgba(15, 23, 42, 0.45);
  opacity: 0;
  transition: opacity 0.15s ease;
}
.img-preview:hover .img-mask {
  opacity: 1;
}
.img-empty {
  padding: 48px 16px;
  text-align: center;
  color: #64748b;
}
.img-empty__label {
  margin-top: 12px;
  font-size: 16px;
  color: #334155;
}
.img-empty__tip {
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
