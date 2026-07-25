<script setup lang="ts">
import { ElMessage } from 'element-plus'
import {
  PLAY_TYPE_OPTIONS,
  TOPIC_TAG_OPTIONS,
  type TopicVO,
} from '@liuhecai/shared'
definePageMeta({ title: '资料管理' })
const { api, hydrate } = useAgentAuth()

const topicList = ref<TopicVO[]>([])
const topicForm = reactive({
  title: '特码必中资料',
  lotteryType: 'MACAU_NEW',
  issueNo: '20260722',
  playType: '特码',
  tag: '出售帖',
  price: 10,
  content: '演示正文：1 12 23 34（购买后可见）',
  status: 1,
})
const statusLabel: Record<number, string> = {
  0: '待审',
  1: '已上架',
  2: '拒绝',
  3: '下架',
}

async function loadTopics() {
  topicList.value = await api<TopicVO[]>('/api/agent/topics')
}

async function createTopic() {
  try {
    await api('/api/agent/topics', { method: 'POST', body: { ...topicForm } })
    ElMessage.success('资料已创建')
    await loadTopics()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  }
}

async function setTopicStatus(id: string, status: number) {
  try {
    await api(`/api/agent/topics/${id}/status`, { method: 'PUT', body: { status } })
    await loadTopics()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '状态更新失败')
  }
}

onMounted(async () => {
  hydrate()
  await loadTopics()
})
</script>

<template>
  <el-row :gutter="16">
    <el-col :xs="24" :md="12">
      <el-card>
        <template #header>发布资料</template>
        <el-form label-position="top">
          <el-form-item label="标题"><el-input v-model="topicForm.title" /></el-form-item>
          <el-form-item label="彩种">
            <el-select v-model="topicForm.lotteryType" style="width:100%">
              <el-option label="新澳门" value="MACAU_NEW" />
              <el-option label="香港" value="HK" />
              <el-option label="老澳门" value="MACAU_OLD" />
            </el-select>
          </el-form-item>
          <el-form-item label="期号"><el-input v-model="topicForm.issueNo" /></el-form-item>
          <el-form-item label="帖子标签">
            <el-select
              v-model="topicForm.tag"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入标签"
              style="width:100%"
            >
              <el-option v-for="opt in TOPIC_TAG_OPTIONS" :key="opt" :label="opt" :value="opt" />
            </el-select>
          </el-form-item>
          <el-form-item label="玩法">
            <el-select
              v-model="topicForm.playType"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入玩法"
              style="width:100%"
            >
              <el-option v-for="opt in PLAY_TYPE_OPTIONS" :key="opt" :label="opt" :value="opt" />
            </el-select>
          </el-form-item>
          <el-form-item label="价格"><el-input-number v-model="topicForm.price" :min="0" /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="topicForm.status" style="width:100%">
              <el-option label="待审" :value="0" />
              <el-option label="直接上架" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="正文">
            <ClientOnly>
              <RichTextEditor v-model="topicForm.content" height="220px" />
            </ClientOnly>
          </el-form-item>
          <el-button type="primary" @click="createTopic">创建</el-button>
        </el-form>
      </el-card>
    </el-col>
    <el-col :xs="24" :md="12">
      <el-card>
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <span>本站资料</span>
            <el-button size="small" @click="loadTopics">刷新</el-button>
          </div>
        </template>
        <el-table :data="topicList" size="small">
          <el-table-column prop="title" label="标题" min-width="120" />
          <el-table-column prop="tag" label="标签" width="80" />
          <el-table-column prop="price" label="价格" width="70" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">{{ statusLabel[row.status ?? 0] }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button link type="success" @click="setTopicStatus(row.id, 1)">上架</el-button>
              <el-button link type="warning" @click="setTopicStatus(row.id, 2)">拒绝</el-button>
              <el-button link type="info" @click="setTopicStatus(row.id, 3)">下架</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>
</template>
