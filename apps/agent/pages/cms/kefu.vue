<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '客服页' })
const { api, hydrate } = useAgentAuth()

const title = ref('客服')
const form = reactive({
  heading: '',
  intro: '',
  qrWechatUrl: '',
  qrQqUrl: '',
})

async function load() {
  const page = await api<{ title: string; content: Record<string, any> }>('/api/agent/cms/pages/kefu')
  title.value = page.title || '客服'
  form.heading = page.content.heading || ''
  form.intro = page.content.intro || ''
  form.qrWechatUrl = page.content.qrWechatUrl || ''
  form.qrQqUrl = page.content.qrQqUrl || ''
}

async function save() {
  try {
    await api('/api/agent/cms/pages/kefu', {
      method: 'PUT',
      body: { title: title.value, content: { ...form } },
    })
    ElMessage.success('客服页已保存')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

onMounted(async () => {
  hydrate()
  await load()
})
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <span>客服页</span>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </template>
    <el-form label-position="top" style="max-width:720px;">
      <el-form-item label="内部备注"><el-input v-model="title" /></el-form-item>
      <el-form-item label="页面标题"><el-input v-model="form.heading" /></el-form-item>
      <el-form-item label="说明">
        <ClientOnly>
          <RichTextEditor v-model="form.intro" height="220px" />
        </ClientOnly>
      </el-form-item>
      <el-form-item label="微信二维码（仅本页展示）"><ImageUploadField v-model="form.qrWechatUrl" /></el-form-item>
      <el-form-item label="QQ 二维码（仅本页展示）"><ImageUploadField v-model="form.qrQqUrl" /></el-form-item>
      <p style="color:#6b7280;">联系微信 / QQ 账号请在「基础设置」中维护（全站文案）。</p>
    </el-form>
  </el-card>
</template>
