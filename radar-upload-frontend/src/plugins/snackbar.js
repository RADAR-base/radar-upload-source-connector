/* eslint-disable no-param-reassign */
/* eslint-disable func-names */

const SnackBar = {
  install(Vue, store) {
    Vue.prototype.$success = function (text, timeout = 2000) {
      store.commit('openSnackbar', { text });
    };

    Vue.prototype.$error = function (text, timeout = 2000) {
      store.commit('openSnackbar', { text, type: 'error' });
    };
  },
};

export default SnackBar;
