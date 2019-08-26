import Vue from 'vue';
import Router from 'vue-router';
import PatientFileTabs from '@/views/contents/PatientFileTabs.vue';
import ProjectListCard from '@/components/Project/ProjectListCard.vue';

Vue.use(Router);

export default new Router({
  routes: [
    {
      path: '',
      component: PatientFileTabs,
      name: 'Patients and Files',
    },
    {
      path: '/projects',
      component: ProjectListCard,
      name: 'ProjectListCard',
    },
  ],
});
