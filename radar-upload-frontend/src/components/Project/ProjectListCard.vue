<template>
  <v-container>
    <v-row>
      <v-col
        cols="12"
        xs="12"
        sm="6"
        md="4"
        lg="3"
      >
        <v-card
          color="primary"
          dark
          v-for="(project, i) in projects"
          :key="i"
        >
          <v-card-title>{{ project.text || project.value }}</v-card-title>
          <v-card-text class="white--text">
            <div class="my-4 subtitle-1">
              {{ project.organization }}
            </div>
            <div class="my-4 subtitle-2 mb-2">
              {{ project.location }}
            </div>
            {{ project.description }}
          </v-card-text>

          <v-card-actions>
            <v-btn
              text
              @click="selectProject(project)"
            >
              Select
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
<script>
import projectMixin from './projectMixin';

export default {
  mixins: [projectMixin],
};
</script>

<style>

</style>
