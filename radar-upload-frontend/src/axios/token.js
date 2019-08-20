/* eslint-disable camelcase */
/**
 * Manage the how Access Tokens are being stored and retreived from storage.
 */
import axios from 'axios';
import config from './index';

const TOKEN = 'token';
// const TOKEN_KEY = 'tokenKey';

const TokenService = {
  async get(authCode, clientId = 'radar_upload_frontend') {
    const headers = {
      'Content-Type': 'application/x-www-form-urlencoded',
      auth: {
        username: clientId,
      },
    };
    // pass data to type x-www-form-urlendcoded
    const params = new URLSearchParams();
    params.append('code', authCode);
    params.append('grant_type', 'authorization_code');
    params.append('redirect_uri', 'http://localhost:8080/login');

    const { access_token } = await axios.post(
      'https://radar-test.thehyve.net/managementportal/oauth/token',
      params,
      headers,
    );
    this.saveToken(access_token);
    config.setHeader();
  },

  saveToken(token) {
    localStorage.setItem(TOKEN, token);
  },

  removeToken() {
    localStorage.removeItem(TOKEN);
  },
};

export default TokenService;
