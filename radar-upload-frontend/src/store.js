import Vue from 'vue';
import Vuex from 'vuex';
import modules, { fileList } from '@/store/index';

Vue.use(Vuex);
const store = new Vuex.Store({
  modules,
  state: {
    message: {
      open: false,
      error: false,
      text: '',
      timeout: 4000,
    },
  },
  mutations: {
    openSnackbar(state, { type, text, timeout }) {
      state.message.open = true;
      state.message.text = text;
      state.message.timeout = timeout;
      state.message.error = type === 'error';
    },
  },
  actions: {

  },
});

// hot reloading for vuex modules
if (module.hot) {
  // accept actions and mutations as hot modules
  module.hot.accept(fileList, () => {
    store.hotUpdate({
      modules,
    });
  });
}

export default store;
