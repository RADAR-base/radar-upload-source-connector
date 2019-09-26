import Vue from 'vue';
import Router from 'vue-router';
import PatientFileTabs from '@/views/contents/PatientFileTabs.vue';
import ProjectListCard from '@/components/Project/ProjectListCard.vue';
import Login from '@/components/Login';

Vue.use(Router);

export default new Router({
  routes: [
    {
      path: '/projects',
      alias: '/',
      component: ProjectListCard,
      name: 'ProjectListCard',
    },
    {
      path: '/projects/:projectId',
      component: PatientFileTabs,
      name: 'Patients and Files',
    },
    {
      path: '/login',
      component: Login,
      name: 'Login',
    },
  ],
});
