<template>
  <v-app id="app">
    <!-- Notification components -->
    <v-snackbar
      v-model="message.open"
      :color="message.error?'error': 'success'"
      top
      :timeout="message.timeout"
    >
      {{ message.text }}
    </v-snackbar>

    <!-- layout -->
    <LeftDrawer
      :drawer="drawer.left"
      @transitionend="val=>drawer.left=val"
      v-if="$route.path!=='/login'"
    />

    <AppBar
      @ToggleLeftDrawer="drawer.left=!drawer.left"
      v-if="$route.path!=='/login'"
    />

    <router-view name="login" />

    <v-content>
      <v-container
        fluid
      >
        <router-view :key="$route.path" />
      </v-container>
    </v-content>

    <!-- <Footer /> -->
  </v-app>
</template>

<script>
import { mapState } from 'vuex';
import LeftDrawer from '@/views/layouts/LeftDrawer';
import AppBar from '@/views/layouts/AppBar';

export default {
  components: {
    LeftDrawer,
    // Footer,
    AppBar,
  },
  name: 'App',
  data: () => ({
    drawer: {
      left: false,
    },
  }),
  computed: {
    ...mapState({
      message: (state) => state.message,
    }),
  },
};

</script>
