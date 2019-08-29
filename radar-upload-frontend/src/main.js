import Vue from 'vue';
import App from './App.vue';
import router from './router';
import store from './store';
import './registerServiceWorker';
import vuetify from './plugins/vuetify';
import './plugins/filter';
import './plugins/vue-moment';
import services from '@/axios/';
import '@/assets/styles/main.scss';
import Snackbar from './plugins/snackbar';
// init api services
services.init(process.env.VUE_APP_BASE_URL, store, router);

Vue.config.productionTip = false;
Vue.use(Snackbar, store);
new Vue({
  router,
  store,
  vuetify,
  render: h => h(App),
}).$mount('#app');