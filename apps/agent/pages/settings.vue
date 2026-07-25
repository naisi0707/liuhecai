<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { FONT_FAMILY_OPTIONS } from '@liuhecai/shared'
definePageMeta({ title: '基础设置' })
const { api, hydrate } = useAgentAuth()
const { loadSiteName } = useAgentSite()
const { previewMediaUrl } = usePreviewMedia()

const form = reactive({
  name: '',
  announcement: '',
  kefuWechat: '',
  kefuQq: '',
  primaryColor: '#c62828',
  fontFamily: 'Microsoft YaHei',
  logoUrl: '',
})

/** 前台公告按纯文本渲染，预览剥离 HTML 标签 */
const announcementPlain = computed(() =>
  (form.announcement || '')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/\n{3,}/g, '\n\n')
    .trim(),
)

const hasPreview = computed(
  () => !!(form.name.trim() || form.logoUrl || announcementPlain.value
    || form.kefuWechat || form.kefuQq),
)

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

    <el-row :gutter="20">
      <el-col :xs="24" :lg="13">
        <el-form label-position="top">
          <el-form-item label="站名"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="公告">
            <ClientOnly>
              <RichTextEditor v-model="form.announcement" height="240px" />
            </ClientOnly>
            <div style="color:#6b7280;font-size:12px;margin-top:6px;">
              前台按纯文本展示；右侧预览已去掉标签，与访客所见一致。
            </div>
          </el-form-item>
          <el-form-item label="联系账号·微信（全站文案）"><el-input v-model="form.kefuWechat" /></el-form-item>
          <el-form-item label="联系账号·QQ（全站文案）"><el-input v-model="form.kefuQq" /></el-form-item>
          <el-form-item label="主色"><el-color-picker v-model="form.primaryColor" /></el-form-item>
          <el-form-item label="字体">
            <el-select
              v-model="form.fontFamily"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入字体"
              style="width:100%"
            >
              <el-option v-for="opt in FONT_FAMILY_OPTIONS" :key="opt" :label="opt" :value="opt" />
            </el-select>
          </el-form-item>
          <el-form-item label="Logo"><ImageUploadField v-model="form.logoUrl" /></el-form-item>
        </el-form>
      </el-col>

      <el-col :xs="24" :lg="11">
        <CmsPreviewShell :empty="!hasPreview">
          <div
            class="settings-prev"
            :style="{
              '--brand': form.primaryColor || '#c62828',
              fontFamily: form.fontFamily || 'Microsoft YaHei',
            }"
          >
            <div class="settings-prev__header">
              <img
                v-if="form.logoUrl"
                class="settings-prev__logo"
                :src="previewMediaUrl(form.logoUrl)"
                alt="logo"
              />
              <div class="settings-prev__name">{{ form.name || '站点名称' }}</div>
            </div>
            <div v-if="announcementPlain" class="settings-prev__announce">
              <div class="settings-prev__announce-track">{{ announcementPlain }}</div>
            </div>
            <div v-if="form.kefuWechat || form.kefuQq" class="settings-prev__contact">
              <template v-if="form.kefuWechat">微信：{{ form.kefuWechat }}</template>
              <span v-if="form.kefuQq">　QQ：{{ form.kefuQq }}</span>
            </div>
            <p class="settings-prev__font-sample">字体示意 · Aa 中文 123</p>
          </div>
        </CmsPreviewShell>
      </el-col>
    </el-row>
  </el-card>
</template>

<style scoped>
.settings-prev {
  margin: 4px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.settings-prev__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #eee;
  background: #fafafa;
}

.settings-prev__logo {
  height: 35px;
  width: auto;
  max-width: 140px;
  object-fit: contain;
}

.settings-prev__name {
  font-weight: 800;
  font-size: 18px;
  color: var(--brand, #c62828);
}

.settings-prev__announce {
  background: #fff8e1;
  border-bottom: 1px solid #ffe082;
  padding: 8px 0;
}

.settings-prev__announce-track {
  padding: 0 12px;
  color: #5d4037;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.settings-prev__contact {
  padding: 10px 14px;
  font-weight: 700;
  color: var(--brand, #c62828);
  background: #f5f5f5;
  font-size: 14px;
}

.settings-prev__font-sample {
  margin: 0;
  padding: 14px;
  color: #666;
  font-size: 15px;
}
</style>
