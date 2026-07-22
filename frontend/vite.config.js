import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5175,
    // 代理：前端 /api 请求转发到后端 8080，解决跨域问题
    proxy: {
      // 页面 8、9 的成员四接口仍运行在 8083；独立代理不改变其他成员的 /api 请求。
      '/member4-api': {
        target: 'http://127.0.0.1:8083',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/member4-api/, '/api')
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
