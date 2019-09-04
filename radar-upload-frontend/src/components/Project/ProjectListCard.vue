<template>
  <v-container>
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

    <v-row>
      <v-col
        cols="12"
        xs="12"
        sm="6"
        md="4"
        lg="3"
        v-for="(project, i) in projects"
        :key="i"
      >
        <v-card elevation="5">
          <v-card-title class="px-2 headline">
            {{ (project.text || project.value) | textTruncate }}
          </v-card-title>

          <v-card-text class="pl-3 pr-1">
            {{ project.description | textTruncate(40) }}
          </v-card-text>

          <v-card-actions>
            <div class="caption">
              {{ project.organization }}
              <br>
              {{ project.location }}
            </div>

            <v-spacer />
            <v-btn
              text
              @click="selectProject(project)"
              color="primary"
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
