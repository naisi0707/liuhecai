<script setup lang="ts">
const route = useRoute()
const { token, logout, hydrate } = useAgentAuth()
const { siteName, loadSiteName, clearSiteName } = useAgentSite()

const menus = [
  {
    title: '运营',
    children: [
      { path: '/', label: '运营看板' },
      { path: '/users', label: '用户管理' },
      { path: '/topics', label: '资料管理' },
      { path: '/recharges', label: '充值确认' },
      { path: '/draws', label: '开奖管理' },
      { path: '/settings', label: '基础设置' },
    ],
  },
  {
    title: '内容管理',
    children: [
      { path: '/cms/menus', label: '导航菜单' },
      { path: '/cms/home', label: '首页' },
      { path: '/cms/rules', label: '规则页' },
      { path: '/cms/recharge', label: '充值页' },
      { path: '/cms/kefu', label: '客服页' },
    ],
  },
]

const active = computed(() => {
  if (route.path === '/') return '/'
  if (route.path.startsWith('/users')) return '/users'
  return route.path
})

const headerTitle = computed(() => {
  const page = String(route.meta.title || '内容管理')
  return siteName.value ? `${siteName.value} · ${page}` : page
})

function onLogout() {
  clearSiteName()
  logout()
  navigateTo('/login')
}

watch(token, (v) => {
  if (v) loadSiteName(true)
  else clearSiteName()
}, { immediate: true })

onMounted(() => {
  hydrate()
  if (token.value) loadSiteName(true)
})
</script>

<template>
  <div v-if="!token" class="login-wrap">
    <slot />
  </div>
  <el-container v-else class="agent-shell">
    <el-aside width="220px" class="agent-aside">
      <div class="brand">
        <div class="brand-title">{{ siteName || '代理后台' }}</div>
        <div class="brand-sub">代理后台</div>
      </div>
      <el-menu :default-active="active" router>
        <el-menu-item-group v-for="g in menus" :key="g.title" :title="g.title">
          <el-menu-item v-for="m in g.children" :key="m.path" :index="m.path">
            {{ m.label }}
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </el-aside>
    <el-container class="agent-body">
      <el-header class="agent-header">
        <span>{{ headerTitle }}</span>
        <el-button text type="danger" @click="onLogout">退出</el-button>
      </el-header>
      <el-main class="agent-main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<style>
.agent-shell {
  height: 100vh;
  overflow: hidden;
  background: #f3f5f8;
}
.agent-aside {
  height: 100%;
  overflow-y: auto;
  background: #fff;
  border-right: 1px solid #e5e7eb;
}
.brand {
  padding: 18px 16px;
  border-bottom: 1px solid #f1f1f1;
}
.brand-title {
  font-weight: 700;
  color: #7f1d1d;
  font-size: 16px;
  line-height: 1.3;
}
.brand-sub { font-size: 12px; color: #6b7280; margin-top: 4px; }
.agent-body {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  flex-direction: column;
}
.agent-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #7f1d1d;
  color: #fff;
}
.agent-main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
}
.login-wrap {
  min-height: 100vh;
  background: #f3f5f8;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 8vh;
}
</style>
