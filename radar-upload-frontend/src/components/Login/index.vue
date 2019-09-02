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
      <v-card class="elevation-12">
        <v-toolbar
          color="primary"
          dark
          flat
        >
          <v-toolbar-title>Login form</v-toolbar-title>
        </v-toolbar>
        <v-card-text>
          <v-form :disabled="loading">
            <v-text-field
              label="Login"
              name="login"
              prepend-icon="mdi-account"
              type="text"
            />

            <v-text-field
              id="password"
              label="Password"
              name="password"
              prepend-icon="mdi-lock"
              type="password"
            />
          </v-form>
        </v-card-text>
        <v-card-actions>
          <div class="flex-grow-1 text-align-center" />
          <div
            class="title pr-2"
            style="color: grey"
          >
            OR
          </div>
          <v-btn
            :loading="loading"
            color="primary"
            @click="redirectLogin"
          >
            Login by management portal
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-col>
  </v-row>
</template>

<script>
/* eslint-disable no-undef */
import { clearInterval } from 'timers';

export default {
  data: () => ({
    loading: false,
  }),
  methods: {
    async redirectLogin() {
      const url = process.env.VUE_APP_AUTH_API || $VUE_APP_AUTH_API;
      const clientId = process.env.VUE_APP_CLIENT_ID || $VUE_APP_CLIENT_ID;
      const authCallback = process.env.VUE_APP_AUTH_CALLBACK || $VUE_APP_AUTH_CALLBACK;
      window.open(`${url}/authorize?client_id=${clientId}&response_type=code&redirect_uri=${authCallback}`);
      this.loading = true;
      // eslint-disable-next-line func-names
      this.checkToken = setInterval(() => {
        if (localStorage.getItem('token')) {
          window.location.replace('');
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
