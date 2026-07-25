<script setup lang="ts">
import { ElMessage } from 'element-plus'

const route = useRoute()
const { token, logout, hydrate, changePassword } = useAgentAuth()
const { siteName, loadSiteName, clearSiteName } = useAgentSite()

const pwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function submitPassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  pwdSaving.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码已修改，请重新登录')
    pwdVisible.value = false
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirm = ''
    clearSiteName()
    logout()
    await navigateTo('/login')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '修改失败')
  } finally {
    pwdSaving.value = false
  }
}

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
        <el-space>
          <el-button text style="color:#fff;" @click="pwdVisible = true">修改密码</el-button>
          <el-button text type="danger" @click="onLogout">退出</el-button>
        </el-space>
      </el-header>
      <el-main class="agent-main">
        <slot />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="pwdVisible" title="修改密码" width="420px" append-to-body>
    <el-form label-width="90px">
      <el-form-item label="旧密码">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="pwdForm.confirm" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdVisible = false">取消</el-button>
      <el-button type="primary" :loading="pwdSaving" @click="submitPassword">确认</el-button>
    </template>
  </el-dialog>
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
