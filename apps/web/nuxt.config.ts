// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  // 开发时关闭，减少控制台/悬浮层噪音
  devtools: { enabled: false },
  css: [
    '~/assets/styles/main.scss',
  ],
  app: {
    head: {
      title: '刘伯温论坛',
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'format-detection', content: 'telephone=no' },
      ],
    },
  },
  build: {
    transpile: ['@liuhecai/shared', 'dompurify'],
  },
  nitro: {
    devProxy: {
      '/uploads': { target: 'http://127.0.0.1:8080/uploads', changeOrigin: true },
    },
  },
  runtimeConfig: {
    public: {
      apiBase: 'http://127.0.0.1:8080',
      webBase: 'http://127.0.0.1:3000',
    },
  },
})
