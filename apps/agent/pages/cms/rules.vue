<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '规则页' })
const { api, hydrate } = useAgentAuth()
const { siteName, loadSiteName } = useAgentSite()

const title = ref('规则')
const form = reactive({
  heading: '',
  intro: '',
  guarantees: [] as Array<{ title: string; body: string }>,
})

const previewName = computed(() => siteName.value || '站点名称')
const hasPreview = computed(
  () => !!(form.heading.trim() || form.intro.trim() || form.guarantees.some((g) => g.title || g.body)),
)

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
  await Promise.all([load(), loadSiteName()])
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

    <el-row :gutter="20">
      <el-col :xs="24" :lg="13">
        <el-form label-position="top">
          <el-form-item label="内部备注"><el-input v-model="title" /></el-form-item>
          <el-form-item label="页面标题"><el-input v-model="form.heading" placeholder="如：承诺与保障" /></el-form-item>
          <el-form-item label="简介（前台显示为「《站名》+简介」）">
            <el-input
              v-model="form.intro"
              type="textarea"
              :rows="3"
              placeholder="如：站长只负责充值与监督，不售码、不荐码；资料由审核通过的高手发表。"
            />
          </el-form-item>
        </el-form>
        <el-divider>保障条目</el-divider>
        <el-button size="small" @click="form.guarantees.push({ title: '', body: '' })">添加</el-button>
        <div
          v-for="(g, i) in form.guarantees"
          :key="i"
          style="margin:12px 0;padding:12px;background:#fafafa;border-radius:6px;"
        >
          <el-input v-model="g.title" placeholder="标题" style="margin-bottom:8px;" />
          <ClientOnly>
            <RichTextEditor v-model="g.body" height="200px" placeholder="保障正文（支持文字/图片）" />
          </ClientOnly>
          <el-button type="danger" text @click="form.guarantees.splice(i, 1)">删除</el-button>
        </div>
      </el-col>

      <el-col :xs="24" :lg="11">
        <CmsPreviewShell :empty="!hasPreview">
          <section class="rules-box">
            <h3 v-if="form.heading" class="rules-box__title">{{ form.heading }}</h3>
            <div v-if="form.intro" class="rules-box__banner">《{{ previewName }}》{{ form.intro }}</div>
            <ol v-if="form.guarantees.length" class="rules-box__list">
              <li v-for="(g, i) in form.guarantees" :key="i">
                <strong v-if="g.title">{{ g.title }}</strong>
                <SafeHtml v-if="g.body" :html="g.body" />
              </li>
            </ol>
          </section>
        </CmsPreviewShell>
      </el-col>
    </el-row>
  </el-card>
</template>

<style scoped>
.rules-box {
  margin: 4px;
  border: 1px solid #2e7d32;
  padding: 16px 18px 20px;
  background: #fff;
  font-family: "Microsoft YaHei", sans-serif;
  color: #222;
}

.rules-box__title {
  margin: 0 0 12px;
  text-align: center;
  font-size: 22px;
}

.rules-box__banner {
  background: #ff8a65;
  color: #fff;
  text-align: center;
  padding: 8px;
  font-weight: 700;
  margin-bottom: 14px;
  line-height: 1.5;
}

.rules-box__list {
  margin: 0;
  padding-left: 0;
  list-style: none;
}

.rules-box__list li {
  margin-bottom: 12px;
  line-height: 1.7;
}

.rules-box__list strong {
  color: #c62828;
  display: block;
  margin-bottom: 4px;
}
</style>
