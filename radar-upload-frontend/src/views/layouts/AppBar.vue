<template>
  <v-app-bar
    app
    color="primary"
    dark
    clipped-left
  >
    <v-app-bar-nav-icon @click.stop="$emit('ToggleLeftDrawer')" />
    <v-toolbar-title>Upload</v-toolbar-title>
    <v-spacer />
  </v-app-bar>
</template>

<script>
export default {

};
</script>

<style>

</style>
