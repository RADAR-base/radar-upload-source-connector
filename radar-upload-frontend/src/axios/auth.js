import axios from 'axios';

export default {
  logout() {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('tokenExpiration');
    document.cookie = 'authorizationBearer=;Max-Age=0';
  },
  login(token) {
    sessionStorage.setItem('token', token.token);
    sessionStorage.setItem('tokenExpiration', token.expirationDate.toISOString());
    const age = (token.expirationDate.getTime() - Date.now()) / 1000;
    document.cookie = `authorizationBearer=${token.token};Max-Age=${age}`;
  },
  async processLogin(authCode, appConfig) {
    // eslint-disable-next-line no-undef
    const tokenResponse = await this.fetchToken(authCode, appConfig);
    this.login(tokenResponse);

    let redirectTo = sessionStorage.getItem('lastAuthorizedPath');
    if (redirectTo) {
      sessionStorage.removeItem('lastAuthorizedPath');
    } else {
      redirectTo = '/';
    }

    return { path: redirectTo };
  },
  getToken() {
    const expiration = sessionStorage.getItem('tokenExpiration');
    if (expiration === null || Date.parse(expiration) <= Date.now()) {
      return null;
    }
    return sessionStorage.getItem('token');
  },
  async fetchToken(authCode, appConfig) {
    const config = {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      auth: {
        username: appConfig.clientId,
        password: '',
      },
    };
    // pass data to type x-www-form-urlendcoded
    const params = new URLSearchParams({
      code: authCode,
      grant_type: 'authorization_code',
      redirect_uri: appConfig.authCallback,
    });

    // eslint-disable-next-line camelcase
    const { access_token: accessToken, expires_in: expiresIn } = await axios.post(
      // eslint-disable-next-line no-undef
      `${appConfig.authAPI}/token`,
      params,
      config,
    );
    // eslint-disable-next-line camelcase
    return {
      token: accessToken,
      expirationDate: new Date(Date.now() + expiresIn * 1000),
    };
  },

  authorizationFailed(store, router) {
    this.logout();

    if (!window.location.href.includes('/login')) {
      const route = window.location.href.split('#').slice(1).join('#') || '/';
      sessionStorage.setItem('lastAuthorizedPath', route);
      // eslint-disable-next-line no-case-declarations
      router.replace({ name: 'Login' });
      store.commit('openSnackbar', { type: 'error', text: 'Please login to continue' });
    }
  },

  authorize(appConfig) {
    window.location.href = `${appConfig.authAPI}/authorize?client_id=${appConfig.clientId}&response_type=code&redirect_uri=${appConfig.authCallback}`;
  },
};
