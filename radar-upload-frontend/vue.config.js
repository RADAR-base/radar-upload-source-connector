// vue.config.js

module.exports = {
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
      config.module.rule('scss').oneOf(match).use('sass-loader')
        .tap(opt => Object.assign(opt, { data: '@import \'~@/assets/styles/main.scss\';' }));
    });
  },
};
