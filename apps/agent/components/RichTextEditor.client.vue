<script setup lang="ts">
/**
 * .client.vue：禁止 SSR 加载 wangEditor。
 * wangEditor 在模块初始化时会写 navigator，Node 里会抛
 * "Cannot set property navigator of #<Object> which has only a getter"，
 * 导致刷新时页面 SSR 失败、短暂露出 {{}}。
 */
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import '@wangeditor/editor/dist/css/style.css'
import { ElMessage } from 'element-plus'

const props = withDefaults(defineProps<{
  modelValue?: string
  height?: string
  placeholder?: string
}>(), {
  modelValue: '',
  height: '280px',
  placeholder: '请输入内容…',
})

const emit = defineEmits<{ 'update:modelValue': [string] }>()

const { authHeaders, hydrate } = useAgentAuth()
const config = useRuntimeConfig()
const apiBase = (config.public.apiBase as string) || ''

const editorRef = shallowRef<IDomEditor>()
const mode = 'default'

const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: ['group-video', 'insertTable', 'codeBlock', 'fullScreen', 'insertImage'],
}

const editorConfig: Partial<IEditorConfig> = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      async customUpload(file: File, insertFn: (url: string, alt: string, href: string) => void) {
        const form = new FormData()
        form.append('file', file)
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
          insertFn(mediaUrl(res.data.url), file.name, '')
        } catch (e: unknown) {
          ElMessage.error(e instanceof Error ? e.message : '图片上传失败')
        }
      },
    },
  },
}

function mediaUrl(path: string) {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  if (path.startsWith('/uploads/')) return `${apiBase}${path}`
  if (path.startsWith('/bbs/')) {
    const webBase = (config.public.webBase as string) || 'http://127.0.0.1:3000'
    return `${webBase.replace(/\/$/, '')}${path}`
  }
  return `${apiBase}${path.startsWith('/') ? '' : '/'}${path}`
}

const valueHtml = computed({
  get: () => props.modelValue || '',
  set: (v: string) => emit('update:modelValue', v),
})

function handleCreated(editor: IDomEditor) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

onMounted(() => {
  hydrate()
})
</script>

<template>
  <div class="rich-editor" style="border:1px solid #ccc;z-index:10;">
    <Toolbar
      style="border-bottom:1px solid #ccc"
      :editor="editorRef"
      :default-config="toolbarConfig"
      :mode="mode"
    />
    <Editor
      v-model="valueHtml"
      :style="{ height, overflowY: 'hidden' }"
      :default-config="editorConfig"
      :mode="mode"
      @on-created="handleCreated"
    />
  </div>
</template>
