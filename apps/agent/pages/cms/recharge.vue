<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '充值页' })
const { api, hydrate } = useAgentAuth()

const title = ref('充值')
const form = reactive({
  heading: '',
  tiers: [] as string[],
  exchangeRate: '',
  declareText: '',
  notes: [] as string[],
  qrWechatUrl: '',
  qrQqUrl: '',
})

async function load() {
  const page = await api<{ title: string; content: Record<string, any> }>('/api/agent/cms/pages/recharge')
  title.value = page.title || '充值'
  form.heading = page.content.heading || ''
  form.tiers = [...(page.content.tiers || [])]
  form.exchangeRate = page.content.exchangeRate || ''
  form.declareText = page.content.declareText || ''
  form.notes = [...(page.content.notes || [])]
  form.qrWechatUrl = page.content.qrWechatUrl || ''
  form.qrQqUrl = page.content.qrQqUrl || ''
}

async function save() {
  try {
    await api('/api/agent/cms/pages/recharge', {
      method: 'PUT',
      body: { title: title.value, content: { ...form } },
    })
    ElMessage.success('充值页已保存')
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
        <span>充值页（前台指引）</span>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </template>
    <el-form label-position="top" style="max-width:720px;">
      <el-form-item label="内部备注"><el-input v-model="title" /></el-form-item>
      <el-form-item label="页面标题"><el-input v-model="form.heading" /></el-form-item>
      <el-form-item label="汇率说明"><el-input v-model="form.exchangeRate" /></el-form-item>
      <el-form-item label="声明">
        <ClientOnly>
          <RichTextEditor v-model="form.declareText" height="220px" />
        </ClientOnly>
      </el-form-item>
      <el-form-item label="微信二维码（仅本页展示）"><ImageUploadField v-model="form.qrWechatUrl" /></el-form-item>
      <el-form-item label="QQ 二维码（仅本页展示）"><ImageUploadField v-model="form.qrQqUrl" /></el-form-item>
    </el-form>

    <el-divider>充值档位</el-divider>
    <el-button size="small" @click="form.tiers.push('')">添加档位</el-button>
    <div v-for="(_, i) in form.tiers" :key="i" style="display:flex;gap:8px;margin:8px 0;">
      <el-input v-model="form.tiers[i]" />
      <el-button type="danger" text @click="form.tiers.splice(i, 1)">删</el-button>
    </div>

    <el-divider>注意事项</el-divider>
    <el-button size="small" @click="form.notes.push('')">添加</el-button>
    <div v-for="(_, i) in form.notes" :key="'n'+i" style="margin:12px 0;padding:12px;background:#fafafa;border-radius:6px;">
      <ClientOnly>
        <RichTextEditor v-model="form.notes[i]" height="160px" placeholder="注意事项" />
      </ClientOnly>
      <el-button type="danger" text @click="form.notes.splice(i, 1)">删</el-button>
    </div>
  </el-card>
</template>
