module.exports = {
  root: true,

  env: {
    node: true,
  },

  extends: [
    'plugin:vue/strongly-recommended',
    'plugin:vue/essential',
    '@vue/airbnb',
  ],

  rules: {
    'no-console': 'off',
    'no-debugger': 'off',
    'import/extensions': 0,
    'linebreak-style': 0,
    'vue/valid-v-slot': ['error', {
      allowModifiers: true,
    }],
  },

  parserOptions: {
    ecmaVersion: 6,
    parser: 'babel-eslint',
  },
};
