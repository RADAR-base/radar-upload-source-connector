<template>
  <v-list
    shaped
    two-line
    subheader
  >
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
  },
};
</script>

<style>

</style>
