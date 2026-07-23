<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '基础设置' })
const { api, hydrate } = useAgentAuth()
const { loadSiteName } = useAgentSite()

const form = reactive({
  name: '',
  announcement: '',
  kefuWechat: '',
  kefuQq: '',
  primaryColor: '#c62828',
  fontFamily: 'Microsoft YaHei',
  logoUrl: '',
})

async function load() {
  const data = await api<any>('/api/agent/site-config')
  Object.assign(form, {
    name: data.name || '',
    announcement: data.announcement || '',
    kefuWechat: data.kefuWechat || '',
    kefuQq: data.kefuQq || '',
    primaryColor: data.primaryColor || '#c62828',
    fontFamily: data.fontFamily || 'Microsoft YaHei',
    logoUrl: data.logoUrl || '',
  })
}

async function save() {
  try {
    await api('/api/agent/site-config', { method: 'PUT', body: { ...form } })
    await loadSiteName(true)
    ElMessage.success('已保存，请刷新前台查看')
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
        <span>基础设置（站点 / 公告 / 换肤）</span>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </template>
    <el-form label-position="top" style="max-width:520px;">
      <el-form-item label="站名"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="公告">
        <ClientOnly>
          <RichTextEditor v-model="form.announcement" height="240px" />
        </ClientOnly>
      </el-form-item>
      <el-form-item label="联系账号·微信（全站文案）"><el-input v-model="form.kefuWechat" /></el-form-item>
      <el-form-item label="联系账号·QQ（全站文案）"><el-input v-model="form.kefuQq" /></el-form-item>
      <el-form-item label="主色"><el-color-picker v-model="form.primaryColor" /></el-form-item>
      <el-form-item label="字体"><el-input v-model="form.fontFamily" /></el-form-item>
      <el-form-item label="Logo"><ImageUploadField v-model="form.logoUrl" /></el-form-item>
    </el-form>
  </el-card>
</template>
