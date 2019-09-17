// vue.config.js
module.exports = {
  publicPath: process.env.VUE_APP_BASE_URL || "VUE_APP_BASE_URL",
  css: {
    loaderOptions: {
      sass: {
        data: '@import "~@/assets/styles/main.scss"',
      },
    },
  },
  //   // config vuetify to use .scss without possible errors
  chainWebpack: (config) => {
    ['vue-modules', 'vue', 'normal-modules', 'normal'].forEach((match) => {
      config.module
        .rule('scss')
        .oneOf(match)
        .use('sass-loader')
        .tap(opt => Object.assign(opt, { data: "@import '~@/assets/styles/main.scss';" }));
    });
  },
  devServer: {
    https: false,
  },
};
