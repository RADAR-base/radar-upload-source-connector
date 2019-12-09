import Vue from 'vue';
import App from './App.vue';
import router from './router';
import store from './store';
import vuetify from './plugins/vuetify';
import './plugins/filter';
import services from '@/axios/';
import '@/assets/styles/main.scss';
import Snackbar from './plugins/snackbar';
import { baseURL } from '@/app.config';
// init api services
// eslint-disable-next-line no-undef
(() => {
  services.axiosInit(baseURL, store, router);
  if (!services.authInit(store, router)) {
    return;
  }

  Vue.config.productionTip = false;
  Vue.use(Snackbar, store);
  new Vue({
    router,
    store,
    vuetify,
    render: h => h(App),
  }).$mount('#app');
})();
