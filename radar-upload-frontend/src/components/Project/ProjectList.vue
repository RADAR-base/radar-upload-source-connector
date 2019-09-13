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
import projectMixin from './projectMixin.js';

export default {
  mixins: [projectMixin],
  methods: {
    selectProjectFromRoute() {
      const { projectId } = this.$route.params;
      if (projectId) {
        const selectedProject = this.projects.find(project => project.value === projectId);
        if (selectedProject) {
          this.selectProject(selectedProject);
          return;
        }
        this.$error('Selected project not found, please select a project to continue');
        this.$router.push('/projects');
      } else {
        this.$router.push('/projects');
      }
    },
  },
  watch: {
    projects: {
      // right after projects list is loaded, check if a project is selected
      handler(projects) {
        if (projects.length > 0 && !this.selectedProject) {
          this.selectProjectFromRoute();
        }
      },
      immediate: true,
    },
    $route: {
      // after component is rendered
      // in case route (projectId in route) is modified manually
      deep: true,
      handler({ params }) {
        const { projectId } = params;
        if (projectId && (projectId !== this.selectedProject)) {
          this.selectProjectFromRoute();
        }
      },
    },
  },
};
</script>

<style>

</style>
