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
// import Footer from '@/views/layouts/Footer';
import AppBar from '@/views/layouts/AppBar';
import { getToken } from '@/helpers';
import { clientId } from '@/app.config';

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
      message: state => state.message,
    }),
  },
  methods: {
    async getAuth() {
      if (window.location.href.includes('login?code=')) {
        const authCode = window.location.search.replace('?code=', '');
        // eslint-disable-next-line no-undef
        const returnedToken = await getToken(authCode, clientId).catch(() => null);
        localStorage.setItem('token', returnedToken);
        window.close();
      }
    },
  },
  created() {
    this.getAuth();
  },
};

</script>
