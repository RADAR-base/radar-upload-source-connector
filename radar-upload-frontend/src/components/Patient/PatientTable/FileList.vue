<template>
  <v-list
    shaped
    two-line
    subheader
  >
    <v-layout justify-center>
      <v-progress-circular
        class="mt-2"
        v-show="loading"
        indeterminate
        color="primary"
      />
      <v-alert
        type="error"
        v-text="error"
        v-show="error"
        dense
        text
      />
    </v-layout>
    <v-subheader>
      Current files
    </v-subheader>

    <v-list-item-group color="primary">
      <v-list-item
        v-for="(file, i) in patientFiles"
        :key="i"
      >
        <v-list-item-icon>
          <v-icon>mdi-music-circle-outline</v-icon>
        </v-list-item-icon>

        <v-list-item-content>
          <v-list-item-title>
            {{ file.fileName }}
          </v-list-item-title>

          <v-list-item-subtitle>
            {{ file.uploadedAt | moment("YYYY/MM/DD") }}
          </v-list-item-subtitle>
        </v-list-item-content>

        <v-list-item-action>
          <v-icon color="success">
            mdi-check-circle
          </v-icon>
        </v-list-item-action>
      </v-list-item>
    </v-list-item-group>
  </v-list>
</template>

<script>
export default {
  props: {
    patientFiles: {
      type: Array,
      required: true,
    },
    error: {
      type: String,
      default: '',
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
};
</script>

<style>

</style>
