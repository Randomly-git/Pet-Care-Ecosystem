import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      vue(),
      vueDevTools()  // 启用 Vue DevTools（页面内嵌组件树调试面板）
    ],

    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },

    server: {
      port: 5173,
      open: true,
      cors: true,
      proxy: {
        // 媒体服务直接代理到 media-backend（跳过Nacos/网关，居南开发时使用）
        '/api/media': {
          target: 'http://localhost:8082',
          changeOrigin: true,
          secure: false
        },
        // 其他请求统一絏网关配置
        '/api': {
          target: 'http://localhost:9000',
          changeOrigin: true,
          secure: false,
        },
        // GraphQL API直接通过网关
        '/graphql': {
          target: 'http://localhost:9000',
          changeOrigin: true,
          secure: false,
          configure: (proxy, options) => {
            proxy.on('proxyReq', (proxyReq, req, res) => {
              // 移除可能导致CORS问题的头
              proxyReq.removeHeader('origin')
              proxyReq.removeHeader('referer')
            })
          }
        },
        // 媒体文件访问也通过网关
        '/uploads': {
          target: 'http://localhost:9000',  // 通过网关访问媒体服务
          changeOrigin: true,
          secure: false
        }
      }
    },

    build: {
      outDir: 'dist',
      sourcemap: mode === 'development',
      minify: 'terser',
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor': ['vue', 'vue-router', 'pinia'],
            'utils': ['axios']
          },
          chunkFileNames: 'js/[name].[hash].js',
          entryFileNames: 'js/[name].[hash].js',
          assetFileNames: 'assets/[name].[hash].[ext]'
        }
      },
      terserOptions: {
        compress: {
          drop_console: mode === 'production',
          drop_debugger: mode === 'production'
        }
      }
    },

    define: {
      __APP_VERSION__: JSON.stringify(env.VITE_APP_VERSION || '1.0.0'),
      __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
      __ENABLE_MOCK__: env.VITE_ENABLE_MOCK === 'true'
    }
  }
})
