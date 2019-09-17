import Vue from 'vue';
import App from './App.vue';
import router from './router';
import store from './store';
import vuetify from './plugins/vuetify';
import './plugins/filter';
import './plugins/vue-moment';
import services from '@/axios/';
import '@/assets/styles/main.scss';
import Snackbar from './plugins/snackbar';
import { baseURL } from '@/app.config';
// init api services
// eslint-disable-next-line no-undef
services.init(baseURL, store, router);

Vue.config.productionTip = false;
Vue.use(Snackbar, store);
new Vue({
  router,
  store,
  vuetify,
  render: h => h(App),
}).$mount('#app');
