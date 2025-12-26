import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [vue()],

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
        // 🌐 统一网关配置 - 所有请求都通过网关:9000
        // 网关会自动路由到对应的微服务
        '/api': {
          target: 'http://localhost:9000',  // 统一指向网关端口
          changeOrigin: true,
          secure: false,
          // 不需要rewrite，网关会处理路由
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
