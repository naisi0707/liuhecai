<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '规则页' })
const { api, hydrate } = useAgentAuth()

const title = ref('规则')
const form = reactive({
  heading: '',
  intro: '',
  guarantees: [] as Array<{ title: string; body: string }>,
})

async function load() {
  const page = await api<{ title: string; content: Record<string, any> }>('/api/agent/cms/pages/rules')
  title.value = page.title || '规则'
  form.heading = page.content.heading || ''
  form.intro = page.content.intro || ''
  form.guarantees = (page.content.guarantees || []).map((g: any) => ({
    title: g.title || '',
    body: g.body || '',
  }))
}

async function save() {
  try {
    await api('/api/agent/cms/pages/rules', {
      method: 'PUT',
      body: { title: title.value, content: { ...form } },
    })
    ElMessage.success('规则已保存')
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
        <span>规则页</span>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </template>
    <el-form label-position="top" style="max-width:720px;">
      <el-form-item label="内部备注"><el-input v-model="title" /></el-form-item>
      <el-form-item label="页面标题"><el-input v-model="form.heading" /></el-form-item>
      <el-form-item label="简介"><el-input v-model="form.intro" /></el-form-item>
    </el-form>
    <el-divider>保障条目</el-divider>
    <el-button size="small" @click="form.guarantees.push({ title: '', body: '' })">添加</el-button>
    <div v-for="(g, i) in form.guarantees" :key="i" style="margin:12px 0;padding:12px;background:#fafafa;border-radius:6px;">
      <el-input v-model="g.title" placeholder="标题" style="margin-bottom:8px;" />
      <ClientOnly>
        <RichTextEditor v-model="g.body" height="200px" placeholder="保障正文（支持文字/图片）" />
      </ClientOnly>
      <el-button type="danger" text @click="form.guarantees.splice(i, 1)">删除</el-button>
    </div>
  </el-card>
</template>
