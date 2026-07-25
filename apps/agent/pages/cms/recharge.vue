<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '充值页' })
const { api, hydrate } = useAgentAuth()
const { previewMediaUrl } = usePreviewMedia()

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
const contact = reactive({ kefuWechat: '', kefuQq: '' })

const hasPreview = computed(
  () => !!(form.heading.trim() || form.tiers.length || form.exchangeRate.trim()
    || form.declareText.trim() || form.notes.some((n) => n.trim())
    || form.qrWechatUrl || form.qrQqUrl || contact.kefuWechat || contact.kefuQq),
)

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
  await Promise.all([load(), loadContact()])
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

    <el-row :gutter="20">
      <el-col :xs="24" :lg="13">
        <el-form label-position="top">
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
      </el-col>

      <el-col :xs="24" :lg="11">
        <CmsPreviewShell :empty="!hasPreview">
          <section class="recharge-box">
            <h3 v-if="form.heading" class="recharge-box__title">{{ form.heading }}</h3>
            <div v-if="contact.kefuWechat || contact.kefuQq" class="recharge-box__contact">
              <template v-if="contact.kefuWechat">充值金币联系：{{ contact.kefuWechat }}</template>
              <span v-if="contact.kefuQq">　QQ：{{ contact.kefuQq }}</span>
            </div>
            <div v-if="form.qrWechatUrl || form.qrQqUrl" class="recharge-box__qr">
              <img v-if="form.qrWechatUrl" :src="previewMediaUrl(form.qrWechatUrl)" alt="微信" width="206" />
              <img v-if="form.qrQqUrl" :src="previewMediaUrl(form.qrQqUrl)" alt="QQ" width="206" />
            </div>
            <ul v-if="form.tiers.length" class="recharge-box__tiers">
              <li v-for="(t, i) in form.tiers" :key="i">{{ t || '（空档位）' }}</li>
            </ul>
            <div v-if="form.exchangeRate" class="recharge-box__rate">{{ form.exchangeRate }}</div>
            <div v-if="form.declareText" class="recharge-box__declare">
              <strong>声明：</strong>
              <SafeHtml :html="form.declareText" />
            </div>
            <div v-for="(n, i) in form.notes" :key="'pn'+i" class="recharge-box__note">
              <SafeHtml v-if="n" :html="n" />
            </div>
            <div class="recharge-box__form-ph">
              <h4>提交充值申请</h4>
              <p>金额 / 渠道 / 备注（预览占位，不可提交）</p>
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

.recharge-box__tiers {
  list-style: none;
  margin: 0;
  padding: 0;
  font-weight: 700;
  line-height: 1.9;
}

.recharge-box__rate {
  display: inline-block;
  margin: 10px 0;
  background: #fff59d;
  color: #c62828;
  padding: 4px 12px;
  font-weight: 700;
}

.recharge-box__declare {
  text-align: left;
  margin: 12px 0 8px;
  line-height: 1.7;
}

.recharge-box__declare strong {
  color: #c62828;
}

.recharge-box__note {
  color: #c62828;
  margin: 4px 0;
  font-weight: 700;
  text-align: left;
}

.recharge-box__form-ph {
  margin-top: 16px;
  text-align: left;
  border-top: 1px dashed #ddd;
  padding-top: 12px;
  color: #9ca3af;
}

.recharge-box__form-ph h4 {
  margin: 0 0 6px;
  color: #6b7280;
}

.recharge-box__form-ph p {
  margin: 0;
  font-size: 13px;
}
</style>
