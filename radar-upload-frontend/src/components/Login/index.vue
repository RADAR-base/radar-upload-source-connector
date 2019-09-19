<template>
  <v-row
    align="center"
    justify="center"
  >
    <v-col
      cols="12"
      sm="8"
      md="4"
    >
      <v-card
        class="elevation-12"
        max-height="200"
      >
        <v-card-title>
          Login
        </v-card-title>
        <v-divider />
        <v-row>
          <v-col
            align="center"
          >
            <v-btn
              :loading="loading"
              color="primary"
              @click="redirectLogin"
            >
              With Management Portal
            </v-btn>
          </v-col>
        </v-row>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>
/* eslint-disable no-undef */
import appConfig from '@/app.config';
import auth from '../../axios/auth';
import services from '../../axios';

export default {
  data: () => ({
    loading: false,
  }),
  methods: {
    redirectLogin() {
      this.loading = true;
      auth.authorize(appConfig);
    },
  },
  beforeMount() {
    const { code } = this.$route.query;
    if (code) {
      this.loading = true;
      console.log(`Loading with code ${code}`);
      auth.processLogin(code, appConfig)
        .then((nextRoute) => {
          console.log('Processed login');
          services.authInit(this.$store, this.$router);
          this.$router.replace(nextRoute);
        })
        .catch((e) => {
          console.log('Failed to log in', e);
          this.$store.commit('openSnackbar', { type: 'error', text: 'Login failed' });
          this.loading = false;
        });
    }
  },
};
</script>
