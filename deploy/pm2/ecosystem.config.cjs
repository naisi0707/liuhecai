const root = '/www/wwwroot/liuhecai'

module.exports = {
  apps: [
    {
      name: 'liuhecai-web',
      cwd: `${root}/apps/web`,
      script: '.output/server/index.mjs',
      interpreter: 'node',
      env: {
        NODE_ENV: 'production',
        HOST: '127.0.0.1',
        PORT: 3000,
        NUXT_API_BASE: 'http://127.0.0.1:8080',
        NUXT_PUBLIC_API_BASE: '',
        NUXT_PUBLIC_WEB_BASE: '',
      },
    },
    {
      name: 'liuhecai-admin',
      cwd: `${root}/apps/admin`,
      script: '.output/server/index.mjs',
      interpreter: 'node',
      env: {
        NODE_ENV: 'production',
        HOST: '127.0.0.1',
        PORT: 3001,
        NUXT_API_BASE: 'http://127.0.0.1:8080',
        NUXT_PUBLIC_API_BASE: '',
      },
    },
    {
      name: 'liuhecai-agent',
      cwd: `${root}/apps/agent`,
      script: '.output/server/index.mjs',
      interpreter: 'node',
      env: {
        NODE_ENV: 'production',
        HOST: '127.0.0.1',
        PORT: 3002,
        NUXT_API_BASE: 'http://127.0.0.1:8080',
        NUXT_PUBLIC_API_BASE: '',
      },
    },
  ],
}
