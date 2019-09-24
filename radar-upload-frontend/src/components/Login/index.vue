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
import { clearInterval } from 'timers';
import { clientId, authCallback, authAPI } from '@/app.config';
import auth from '@/axios/auth';

export default {
  data: () => ({
    loading: false,
  }),
  methods: {
    async redirectLogin() {
      window.open(`${authAPI}/authorize?client_id=${clientId}&response_type=code&redirect_uri=${authCallback}`);
      this.loading = true;
      // eslint-disable-next-line func-names
      this.checkToken = setInterval(() => {
        if (localStorage.getItem('token')) {
          window.location.replace('');
          auth.login();
        }
      }, 500);
    },
  },
  beforeDestroy() {
    this.loading = false;
    clearInterval(this.checkToken);
  },
};
</script>
