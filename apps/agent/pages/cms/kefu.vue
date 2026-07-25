<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '客服页' })
const { api, hydrate } = useAgentAuth()
const { previewMediaUrl } = usePreviewMedia()

const title = ref('客服')
const form = reactive({
  heading: '',
  intro: '',
  qrWechatUrl: '',
  qrQqUrl: '',
})
const contact = reactive({ kefuWechat: '', kefuQq: '' })

const hasPreview = computed(
  () => !!(form.heading.trim() || form.intro.trim() || form.qrWechatUrl || form.qrQqUrl
    || contact.kefuWechat || contact.kefuQq),
)

async function load() {
  const page = await api<{ title: string; content: Record<string, any> }>('/api/agent/cms/pages/kefu')
  title.value = page.title || '客服'
  form.heading = page.content.heading || ''
  form.intro = page.content.intro || ''
  form.qrWechatUrl = page.content.qrWechatUrl || ''
  form.qrQqUrl = page.content.qrQqUrl || ''
}

async function loadContact() {
  try {
    const data = await api<any>('/api/agent/site-config')
    contact.kefuWechat = data.kefuWechat || ''
    contact.kefuQq = data.kefuQq || ''
  } catch {
    /* ignore */
  }
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
  await Promise.all([load(), loadContact()])
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

    <el-row :gutter="20">
      <el-col :xs="24" :lg="13">
        <el-form label-position="top">
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
      </el-col>

      <el-col :xs="24" :lg="11">
        <CmsPreviewShell :empty="!hasPreview">
          <section class="recharge-box">
            <h3 v-if="form.heading" class="recharge-box__title">{{ form.heading }}</h3>
            <SafeHtml v-if="form.intro" :html="form.intro" class="recharge-box__intro" />
            <div v-if="contact.kefuWechat || contact.kefuQq" class="recharge-box__contact">
              <template v-if="contact.kefuWechat">充值金币联系：{{ contact.kefuWechat }}</template>
              <span v-if="contact.kefuQq">　QQ：{{ contact.kefuQq }}</span>
            </div>
            <div v-if="form.qrWechatUrl || form.qrQqUrl" class="recharge-box__qr">
              <img v-if="form.qrWechatUrl" :src="previewMediaUrl(form.qrWechatUrl)" alt="微信" width="206" />
              <img v-if="form.qrQqUrl" :src="previewMediaUrl(form.qrQqUrl)" alt="QQ" width="206" />
            </div>
          </section>
        </CmsPreviewShell>
      </el-col>
    </el-row>
  </el-card>
</template>

<style scoped>
.recharge-box {
  margin: 4px;
  border: 1px solid #2e7d32;
  padding: 14px 16px 20px;
  text-align: center;
  background: #fff;
  font-family: "Microsoft YaHei", sans-serif;
  color: #222;
}

.recharge-box__title {
  margin: 0 0 12px;
  font-size: 20px;
}

.recharge-box__intro {
  text-align: left;
  margin: 8px 0;
  display: block;
}

.recharge-box__contact {
  background: #f5f5f5;
  color: #c62828;
  padding: 8px;
  margin-bottom: 12px;
  font-weight: 700;
}

.recharge-box__qr {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin: 0 auto 12px;
}

.recharge-box__qr img {
  max-width: 206px;
  height: auto;
  border: 1px dashed #bbb;
  background: #fafafa;
}
</style>
