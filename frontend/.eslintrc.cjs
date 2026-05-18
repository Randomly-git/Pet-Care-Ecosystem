module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true
  },
  extends: [
    'plugin:vue/vue3-recommended',   // Vue 3 推荐规则集
    '@vue/eslint-config-standard'
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module'
  },
  rules: {
    'vue/multi-word-component-names': 'warn',
    'vue/no-unused-vars': 'warn',
    'vue/require-v-for-key': 'error',     // v-for 必须有 :key（防御性）
    'vue/no-v-html': 'warn',             // v-html 有 XSS 风险
    'vue/component-definition-name-casing': ['error', 'PascalCase']
  }
}
