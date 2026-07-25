<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '首页' })
const { api, hydrate } = useAgentAuth()
const { previewMediaUrl } = usePreviewMedia()

const title = ref('首页')
const form = reactive({
  bannerUrl: '',
  drawIframeUrl: '',
  liveIframeUrl: '',
  domainBadge: '',
  showLocalDrawPanel: false,
  qrWechatUrl: '',
  qrQqUrl: '',
  bottomImages: [] as Array<{ src: string; alt: string }>,
})

const hasPreview = computed(
  () => !!(form.bannerUrl || form.domainBadge.trim() || form.drawIframeUrl.trim()
    || form.liveIframeUrl.trim() || form.showLocalDrawPanel
    || form.qrWechatUrl || form.qrQqUrl
    || form.bottomImages.some((i) => i.src)),
)

async function load() {
  const page = await api<{ title: string; content: Record<string, any> }>('/api/agent/cms/pages/home')
  title.value = page.title || '首页'
  Object.assign(form, {
    bannerUrl: page.content.bannerUrl || '',
    drawIframeUrl: page.content.drawIframeUrl || '',
    liveIframeUrl: page.content.liveIframeUrl || '',
    domainBadge: page.content.domainBadge || '',
    showLocalDrawPanel: page.content.showLocalDrawPanel === true,
    qrWechatUrl: page.content.qrWechatUrl || '',
    qrQqUrl: page.content.qrQqUrl || '',
    bottomImages: (page.content.bottomImages || []).map((i: any) => ({
      src: i.src || '',
      alt: i.alt || '',
    })),
  })
}

async function save() {
  try {
    await api('/api/agent/cms/pages/home', {
      method: 'PUT',
      body: { title: title.value, content: { ...form } },
    })
    ElMessage.success('首页内容已保存')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

function addImage() {
  form.bottomImages.push({ src: '', alt: '' })
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
        <span>首页</span>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </template>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="13">
        <el-form label-position="top">
          <el-form-item label="页面标题"><el-input v-model="title" /></el-form-item>
          <el-form-item label="Banner"><ImageUploadField v-model="form.bannerUrl" /></el-form-item>
          <el-form-item label="开奖 iframe"><el-input v-model="form.drawIframeUrl" /></el-form-item>
          <el-form-item label="直播 iframe"><el-input v-model="form.liveIframeUrl" /></el-form-item>
          <el-form-item label="域名条"><el-input v-model="form.domainBadge" /></el-form-item>
          <el-form-item label="显示本地开奖面板">
            <el-switch v-model="form.showLocalDrawPanel" />
            <div style="color:#6b7280;font-size:12px;margin-top:6px;">
              外链开奖 iframe 与本地面板不要同时开启，否则首页会出现两块开奖区
            </div>
          </el-form-item>
          <el-form-item label="微信二维码（仅本页展示）"><ImageUploadField v-model="form.qrWechatUrl" /></el-form-item>
          <el-form-item label="QQ 二维码（仅本页展示）"><ImageUploadField v-model="form.qrQqUrl" /></el-form-item>
        </el-form>

        <el-divider>底图列表（前台资料列表下方长图区）</el-divider>
        <p style="color:#6b7280;font-size:13px;margin:0 0 8px;">对应用户端首页「精选资料」列表后面的长图/广告图，上传后保存即可在前台看到。</p>
        <el-button size="small" @click="addImage">添加</el-button>
        <div v-for="(img, i) in form.bottomImages" :key="i" style="margin:12px 0;padding:12px;background:#fafafa;border-radius:6px;">
          <ImageUploadField v-model="img.src" />
          <el-input v-model="img.alt" placeholder="alt" style="margin-top:8px;" />
          <el-button type="danger" text @click="form.bottomImages.splice(i, 1)">删</el-button>
        </div>
      </el-col>

      <el-col :xs="24" :lg="11">
        <CmsPreviewShell :empty="!hasPreview">
          <div class="home-prev">
            <img
              v-if="form.bannerUrl"
              class="home-prev__banner"
              :src="previewMediaUrl(form.bannerUrl)"
              alt="banner"
            />
            <div v-if="form.domainBadge" class="home-prev__domain">{{ form.domainBadge }}</div>
            <div v-if="form.drawIframeUrl" class="home-prev__iframe-ph">开奖 iframe</div>
            <div v-if="form.showLocalDrawPanel" class="home-prev__iframe-ph">本地开奖面板</div>
            <div v-if="form.liveIframeUrl" class="home-prev__iframe-ph">直播 iframe</div>
            <div v-if="form.qrWechatUrl || form.qrQqUrl" class="home-prev__qr">
              <img v-if="form.qrWechatUrl" :src="previewMediaUrl(form.qrWechatUrl)" alt="微信" />
              <img v-if="form.qrQqUrl" :src="previewMediaUrl(form.qrQqUrl)" alt="QQ" />
            </div>
            <div v-if="form.bottomImages.some((i) => i.src)" class="home-prev__gallery">
              <img
                v-for="(img, i) in form.bottomImages.filter((x) => x.src)"
                :key="i"
                :src="previewMediaUrl(img.src)"
                :alt="img.alt || '底图'"
              />
            </div>
          </div>
        </CmsPreviewShell>
      </el-col>
    </el-row>
  </el-card>
</template>

<style scoped>
.home-prev {
  margin: 4px;
  background: #fff;
  border: 1px solid #e5e7eb;
  padding: 8px;
  font-family: "Microsoft YaHei", sans-serif;
}

.home-prev__banner {
  display: block;
  width: 100%;
  height: auto;
  margin-bottom: 8px;
}

.home-prev__domain {
  background: #c62828;
  color: #fff;
  text-align: center;
  font-weight: 700;
  padding: 6px 8px;
  margin-bottom: 8px;
  font-size: 13px;
}

.home-prev__iframe-ph {
  margin: 6px 0;
  padding: 28px 12px;
  text-align: center;
  color: #6b7280;
  background: #eceff1;
  border: 1px dashed #b0bec5;
  font-size: 13px;
}

.home-prev__qr {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin: 10px 0;
}

.home-prev__qr img {
  width: 120px;
  height: auto;
  border: 1px dashed #bbb;
}

.home-prev__gallery img {
  display: block;
  width: 100%;
  height: auto;
  margin-top: 8px;
}
</style>
