<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '开站创建' })
const { api, hydrate } = useAdminAuth()

const loading = ref(false)
const form = reactive({
  name: '神算子论坛',
  primaryHost: 'ssz.local',
  agentUsername: 'agent_ssz',
  announcement: '欢迎来到神算子（演示新建站）',
})
const result = ref<any>(null)

async function createTenant() {
  loading.value = true
  result.value = null
  try {
    result.value = await api('/api/admin/tenants', {
      method: 'POST',
      body: { ...form },
    })
    ElMessage.success('站点已创建，请保存下方代理密码')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => hydrate())
</script>

<template>
  <el-row :gutter="16">
    <el-col :xs="24" :md="12">
      <el-card>
        <template #header>创建站点</template>
        <el-form label-position="top">
          <el-form-item label="站名"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="主域名"><el-input v-model="form.primaryHost" placeholder="xxx.local" /></el-form-item>
          <el-form-item label="代理用户名"><el-input v-model="form.agentUsername" /></el-form-item>
          <el-form-item label="公告"><el-input v-model="form.announcement" type="textarea" :rows="3" /></el-form-item>
          <el-button type="primary" :loading="loading" @click="createTenant">创建并生成代理密码</el-button>
        </el-form>
      </el-card>
    </el-col>
    <el-col :xs="24" :md="12">
      <el-card v-if="result">
        <template #header>创建成功 · 请立即保存凭据</template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="站名">{{ result.tenant?.name }}</el-descriptions-item>
          <el-descriptions-item label="租户 ID">{{ result.tenant?.id }}</el-descriptions-item>
          <el-descriptions-item label="代理账号">{{ result.agent?.username }}</el-descriptions-item>
          <el-descriptions-item label="代理密码">
            <strong style="color:#b91c1c;font-size:18px;">{{ result.agent?.rawPassword }}</strong>
          </el-descriptions-item>
        </el-descriptions>
        <pre class="echo" style="margin-top:12px;">{{ JSON.stringify(result, null, 2) }}</pre>
      </el-card>
      <el-card v-else>
        <template #header>说明</template>
        <p style="color:#6b7280;line-height:1.7;">
          创建后会自动写入默认 CMS 内容与代理账号。明文密码仅在此回显一次，请复制保存。        </p>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.echo {
  margin: 0;
  background: #0f172a;
  color: #e2e8f0;
  padding: 12px;
  overflow: auto;
  border-radius: 6px;
  font-size: 12px;
}
</style>
