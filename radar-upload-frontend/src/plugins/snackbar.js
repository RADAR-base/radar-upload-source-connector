/* eslint-disable no-param-reassign */
/* eslint-disable func-names */

const SnackBar = {
  install(Vue, store) {
    Vue.prototype.$success = function (text, timeout = 4000) {
      store.commit('openSnackbar', { text, timeout });
    };

    Vue.prototype.$error = function (text, timeout = 4000) {
      store.commit('openSnackbar', { text, type: 'error', timeout });
    };
  },
};

export default SnackBar;
