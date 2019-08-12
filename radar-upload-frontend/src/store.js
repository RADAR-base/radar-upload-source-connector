import Vue from 'vue';
import Vuex from 'vuex';
import modules, { fileList } from '@/store/index';

Vue.use(Vuex);
const store = new Vuex.Store({
  modules,
  state: {

  },
  mutations: {

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
