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

export default {
  data: () => ({
    loading: false,
  }),
  methods: {
    async redirectLogin() {
      window.open('https://radar-test.thehyve.net/managementportal/oauth/authorize?client_id=radar_upload_frontend&response_type=code&redirect_uri=http://localhost:8080/login');
      this.loading = true;
      const checkToken = setInterval(() => {
        if (localStorage.getItem('token')) {
          clearInterval(checkToken);
          this.loading = false;
          this.$router.replace('/');
        }
      }, 500);
    },
  },
};
</script>
