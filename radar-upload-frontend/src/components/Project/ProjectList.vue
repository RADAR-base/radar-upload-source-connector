<template>
  <v-list shaped>
    <v-layout justify-center>
      <v-progress-circular
        class="mt-2"
        v-show="loading"
        indeterminate
        color="primary"
      />
      <v-alert
        type="error"
        v-text="errorMessage"
        v-show="errorMessage"
        dense
        text
      />
    </v-layout>
    <v-list-item-group
      color="primary"
      v-model="selectedProject"
    >
      <v-list-item
        v-for="(project, i) in projects"
        :key="i"
        @click.native="selectProject(project)"
      >
        <v-list-item-icon>
          <v-icon
            color="info"
          >
            mdi-folder-text-outline
          </v-icon>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title>
            {{ project.text }}
          </v-list-item-title>
        </v-list-item-content>
      </v-list-item>
    </v-list-item-group>
  </v-list>
</template>

<script>
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
      });
      this.loading = false;
      if (projects) {
        this.errorMessage = '';
        this.projects = projects.map(el => ({
          text: el.name || el.id,
          value: el.id,
        }));
      }
    },
    selectProject(project) {
      this.$store.commit('project/setCurrentProject', project);
    },
  },
  created() {
    this.getProjects();
  },
};
</script>

<style>

</style>
