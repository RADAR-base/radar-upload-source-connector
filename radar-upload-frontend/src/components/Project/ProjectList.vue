<template>
  <v-list shaped>
    <v-list-item-group
      color="primary"
      v-model="selectedProject"
    >
      <v-list-item
        v-for="(project, i) in projects"
        :key="i"
        @click.native="selectProject(project.value)"
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
    };
  },
  methods: {
    async getProjects() {
      const projects = await api.getProjects();
      this.projects = projects.map(el => ({
        text: el.name,
        value: el.id,
      }));
    },
    selectProject(id) {
      this.$store.dispatch('project/selectProject', id);
    },
  },
  created() {
    // this.getProjects();
  },
};
</script>

<style>

</style>
