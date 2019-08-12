import Vue from 'vue';
import Router from 'vue-router';
import PatientFileTabs from '@/views/contents/PatientFileTabs.vue';

Vue.use(Router);

export default new Router({
  routes: [
    {
      path: '',
      component: PatientFileTabs,
      name: 'Patients and Files',
    },
  ],
});
