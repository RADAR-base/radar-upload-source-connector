<template>
  <v-app id="app">
    <LeftDrawer
      :drawer="drawer.left"
      @transitionend="val=>drawer.left=val"
    />

    <AppBar
      @ToggleLeftDrawer="drawer.left=!drawer.left"
    />

    <v-content>
      <v-container
        fluid
      >
        <router-view />
      </v-container>
    </v-content>

    <!-- <Footer /> -->
  </v-app>
</template>

<script>
import LeftDrawer from '@/views/layouts/LeftDrawer';
import Footer from '@/views/layouts/Footer';
import AppBar from '@/views/layouts/AppBar';

export default {
  components: {
    LeftDrawer,
    Footer,
    AppBar,
  },
  name: 'App',
  data: () => ({
    drawer: {
      left: true,
    },
  }),
};

</script>
