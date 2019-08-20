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
import { mapState } from 'vuex';
import LeftDrawer from '@/views/layouts/LeftDrawer';
import Footer from '@/views/layouts/Footer';
import AppBar from '@/views/layouts/AppBar';
import token from '@/axios/token';

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
    // message: {
    //   open: false,
    //   error: false,
    //   text: '',
    //   timeout: 2000,
    // },

  }),
  computed: {
    ...mapState({
      message: state => state.message,
    }),
  },
  methods: {
    async getToken() {
      const authCode = window.location.search.replace('?code=', '');
      await token.get(authCode);
    },
  },
  created() {
    const currentToken = localStorage.getItem('token');
    if (!window.location.href.includes('login?code=') && !currentToken) {
      window.open('https://radar-test.thehyve.net/managementportal/oauth/authorize?client_id=radar_upload_frontend&response_type=code&redirect_uri=http://localhost:8080/login');
      window.close();
    } else if (!currentToken) {
      this.getToken();
    }
  },
};

</script>
