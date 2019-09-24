import api from '@/axios/project.js';

export default {
  data() {
    return {
      selectedProject: '',
      projects: [],
      loading: false,
      errorMessage: '',
    };
  },
  methods: {
    async getProjects() {
      this.loading = true;
      const projects = await api.getProjects().catch(() => {
        this.errorMessage = 'Loading project fails, please try again later';
        return [];
      });
      this.loading = false;
      this.projects = projects;
    },
    selectProject(project) {
      this.$store.commit('project/setCurrentProject', project);
      this.$router.push(`/projects/${project.value}`);
    },
  },
  created() {
    this.getProjects();
  },
};
