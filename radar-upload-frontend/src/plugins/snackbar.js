/* eslint-disable no-param-reassign */
/* eslint-disable func-names */

const SnackBar = {
  install(Vue) {
    Vue.prototype.$success = function (text, timeout = 2000) {
      this.$data.message.error = false;
      this.$data.message.open = true;
      this.$data.message.text = text;
      this.$data.message.timeout = timeout;
    };

    Vue.prototype.$error = function (text, timeout = 2000) {
      this.$data.message.error = true;
      this.$data.message.open = true;
      this.$data.message.text = text;
      this.$data.message.timeout = timeout;
    };
  },
};

export default SnackBar;
