import pluginVue from 'eslint-plugin-vue'

export default [
  {
    ignores: ['**/dist/**', '**/node_modules/**', '*.config.js']
  },

  // Vue 3 基础规则 (flat config format)
  ...pluginVue.configs['flat/essential'],

  // 自定义规则覆盖
  {
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/no-unused-vars': 'warn',
      'vue/require-v-for-key': 'error',
      'vue/no-v-html': 'warn',
      'vue/component-definition-name-casing': ['error', 'PascalCase'],
      'vue/no-mutating-props': 'error',
      'vue/valid-v-model': 'error',
      'vue/require-prop-types': 'warn',
      'vue/no-multiple-template-root': 'off'
    }
  }
]
