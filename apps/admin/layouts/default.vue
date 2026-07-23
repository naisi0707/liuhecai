<script setup lang="ts">
const route = useRoute()
const { token, logout, hydrate } = useAdminAuth()

const menus = [
  {
    title: '总览',
    children: [
      { path: '/', label: '总览' },
    ],
  },
  {
    title: '用户与代理',
    children: [
      { path: '/agents', label: '代理管理' },
      { path: '/users', label: '用户管理' },
      { path: '/audit-logs', label: '操作审计' },
    ],
  },
  {
    title: '站点',
    children: [
      { path: '/tenants', label: '站点列表' },
      { path: '/domains', label: '域名绑定' },
    ],
  },
]

const active = computed(() => {
  if (route.path.startsWith('/tenants')) return '/tenants'
  if (route.path.startsWith('/agents')) return '/agents'
  if (route.path.startsWith('/users')) return '/users'
  if (route.path.startsWith('/audit-logs')) return '/audit-logs'
  return route.path
})

function onLogout() {
  logout()
  navigateTo('/login')
}

onMounted(() => hydrate())
</script>

<template>
  <div v-if="!token" class="login-wrap">
    <slot />
  </div>
  <el-container v-else class="admin-shell">
    <el-aside width="220px" class="admin-aside">
      <div class="brand">
        <div class="brand-title">超管后台</div>
        <div class="brand-sub">开站 · 代理 · 用户</div>
      </div>
      <el-menu :default-active="active" router>
        <el-menu-item-group v-for="g in menus" :key="g.title" :title="g.title">
          <el-menu-item v-for="m in g.children" :key="m.path" :index="m.path">
            {{ m.label }}
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </el-aside>
    <el-container class="admin-body">
      <el-header class="admin-header">
        <span>{{ route.meta.title || '总览' }}</span>
        <el-button text type="danger" @click="onLogout">退出</el-button>
      </el-header>
      <el-main class="admin-main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<style>
.admin-shell {
  height: 100vh;
  overflow: hidden;
  background: #f3f5f8;
}
.admin-aside {
  height: 100%;
  overflow-y: auto;
  background: #fff;
  border-right: 1px solid #e5e7eb;
}
.brand {
  padding: 18px 16px;
  border-bottom: 1px solid #f1f1f1;
}
.brand-title { font-weight: 700; color: #7f1d1d; }
.brand-sub { font-size: 12px; color: #6b7280; margin-top: 4px; }
.admin-body {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  flex-direction: column;
}
.admin-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #7f1d1d;
  color: #fff;
}
.admin-main {
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
