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
};
</script>

<style>

</style>
