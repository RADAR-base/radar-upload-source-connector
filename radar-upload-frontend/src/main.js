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
// init api services
services.init('api/enpoints', store, router);

Vue.config.productionTip = false;

new Vue({
  router,
  store,
  vuetify,
  render: h => h(App),
}).$mount('#app');
