// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  // 开发时关闭，减少控制台/悬浮层噪音
  devtools: { enabled: false },
  experimental: {
    // 避免 Vite 冷启动 #app-manifest 解析失败导致客户端白屏
    appManifest: false,
  },
  css: [
    '~/assets/styles/main.scss',
  ],
  app: {
    head: {
      // 具体标题由 layouts/default.vue 按 domainRole 设置（ENTRY→导航 / FORUM→站点名）
      title: '导航',
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'format-detection', content: 'telephone=no' },
      ],
    },
  },
  routeRules: {
    // 登录/注册无 SEO 价值，走 CSR 减轻 SSR
    '/login': { ssr: false },
    '/register': { ssr: false },
  },
  build: {
    transpile: ['@liuhecai/shared', 'dompurify'],
  },
  nitro: {
    compressPublicAssets: true,
    devProxy: {
      '/uploads': { target: 'http://127.0.0.1:8080/uploads', changeOrigin: true },
    },
  },
  runtimeConfig: {
    // SSR 直连本机 API；浏览器用 public.apiBase（生产可设为空走同源 /api）
    apiBase: 'http://127.0.0.1:8080',
    public: {
      apiBase: 'http://127.0.0.1:8080',
      webBase: 'http://127.0.0.1:3000',
    },
  },
})
