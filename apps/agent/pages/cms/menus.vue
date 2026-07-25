<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '导航菜单' })
const { api, hydrate } = useAgentAuth()
const { siteName, loadSiteName } = useAgentSite()

type MenuItem = {
  code: string
  title: string
  path: string
  sortNo: number
  visible: number
}

const items = ref<MenuItem[]>([])
const loading = ref(false)

const visibleMenus = computed(() =>
  [...items.value]
    .filter((m) => m.visible === 1)
    .sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0)),
)

const iconPath: Record<string, string> = {
  home: 'M3 10.5 12 3l9 7.5V21a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1v-10.5z',
  rules: 'M7 3h10a2 2 0 0 1 2 2v14l-3-2-3 2-3-2-3 2V5a2 2 0 0 1 2-2zm2 5h6v2H9V8zm0 4h6v2H9v-2z',
  recharge: 'M4 7a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7zm3 3h10v2H7v-2z',
  kefu: 'M12 12a4 4 0 1 0-4-4 4 4 0 0 0 4 4zm0 2c-4 0-7 2-7 4v1h14v-1c0-2-3-4-7-4z',
}

function iconFor(code: string) {
  return iconPath[code] || iconPath.home
}

async function load() {
  loading.value = true
  try {
    items.value = await api<MenuItem[]>('/api/agent/cms/menus')
  } finally {
    loading.value = false
  }
}

async function save() {
  try {
    items.value = await api<MenuItem[]>('/api/agent/cms/menus', {
      method: 'PUT',
      body: { items: items.value },
    })
    ElMessage.success('菜单已保存')
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
  <el-card v-loading="loading">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <span>导航菜单</span>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </template>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="13">
        <el-table :data="items">
          <el-table-column prop="code" label="代码" width="110" />
          <el-table-column label="标题" min-width="140">
            <template #default="{ row }"><el-input v-model="row.title" /></template>
          </el-table-column>
          <el-table-column label="路径" min-width="140">
            <template #default="{ row }"><el-input v-model="row.path" /></template>
          </el-table-column>
          <el-table-column label="排序" width="110">
            <template #default="{ row }"><el-input-number v-model="row.sortNo" :min="0" :controls="false" /></template>
          </el-table-column>
          <el-table-column label="显示" width="90">
            <template #default="{ row }">
              <el-switch v-model="row.visible" :active-value="1" :inactive-value="0" />
            </template>
          </el-table-column>
        </el-table>
      </el-col>

      <el-col :xs="24" :lg="11">
        <CmsPreviewShell :empty="!visibleMenus.length" empty-text="暂无可见菜单，打开「显示」后会在此预览">
          <div class="menu-prev">
            <div class="menu-prev__desktop">
              <div class="menu-prev__brand">{{ siteName || '站点名称' }}</div>
              <ul class="menu-prev__nav">
                <li v-for="m in visibleMenus" :key="m.code">
                  <span>{{ m.title || m.code }}</span>
                </li>
              </ul>
              <div class="menu-prev__actions">
                <span class="menu-prev__btn outline">注册</span>
                <span class="menu-prev__btn solid">登录</span>
              </div>
            </div>
            <p class="menu-prev__label">底部 Tab（移动端）</p>
            <ul class="menu-prev__tabs">
              <li v-for="m in visibleMenus" :key="'t-' + m.code">
                <svg class="menu-prev__icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                  <path :d="iconFor(m.code)" />
                </svg>
                <span>{{ m.title || m.code }}</span>
              </li>
            </ul>
          </div>
        </CmsPreviewShell>
      </el-col>
    </el-row>
  </el-card>
</template>

<style scoped>
.menu-prev {
  margin: 4px;
  background: #fff;
  border: 1px solid #e5e7eb;
  padding: 10px;
  font-family: "Microsoft YaHei", sans-serif;
}

.menu-prev__desktop {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  background: #fafafa;
}

.menu-prev__brand {
  font-weight: 800;
  color: #c62828;
  font-size: 16px;
}

.menu-prev__nav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
}

.menu-prev__nav span {
  color: #333;
  font-size: 14px;
  font-weight: 600;
}

.menu-prev__actions {
  display: flex;
  gap: 8px;
}

.menu-prev__btn {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
}

.menu-prev__btn.outline {
  border: 1px solid #c62828;
  color: #c62828;
}

.menu-prev__btn.solid {
  background: #c62828;
  color: #fff;
}

.menu-prev__label {
  margin: 14px 0 8px;
  font-size: 12px;
  color: #6b7280;
}

.menu-prev__tabs {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
  border-top: 1px solid #e5e7eb;
  background: #fff;
}

.menu-prev__tabs li {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 10px 4px;
  font-size: 12px;
  color: #555;
}

.menu-prev__icon {
  width: 20px;
  height: 20px;
  color: #c62828;
}
</style>
