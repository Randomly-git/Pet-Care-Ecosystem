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
        // PetCare 后端 API (宠物相关)
        '/api/pets': {
          target: 'http://localhost:8082',
          changeOrigin: true,
          secure: false
        },
        '/api/activities': {
          target: 'http://localhost:8082',
          changeOrigin: true,
          secure: false
        },
        '/api/users': {
          target: 'http://localhost:8082',
          changeOrigin: true,
          secure: false
        },
        '/api/auth': {
          target: 'http://localhost:8082',
          changeOrigin: true,
          secure: false
        },
        // 媒体后端 API
        '/api/media': {
          target: 'http://localhost:8081',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api\/media/, '/api/v1/media'),
          secure: false
        },
        // 社区后端 API
        '/api/community': {
          target: 'http://localhost:8084',
          changeOrigin: true,
          rewrite: (path) => {
            // 将 /api/community/moments 转换为 /api/v1/moments
            if (path.startsWith('/api/community/moments')) {
              return path.replace('/api/community', '/api/v1')
            }
            // 将 /api/community/xxx 转换为 /api/v1/xxx
            return path.replace('/api/community', '/api/v1')
          },
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