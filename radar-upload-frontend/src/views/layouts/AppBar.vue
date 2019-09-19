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
    <div
      @click="logout"
      style="cursor: pointer"
    >
      <v-icon>mdi-logout</v-icon>
      Log out
    </div>
  </v-app-bar>
</template>

<script>
import auth from '@/axios/auth';

export default {
  methods: {
    logout() {
      localStorage.removeItem('token');
      this.$router.replace('/login');
      auth.logout();
    },
  },
};
</script>
