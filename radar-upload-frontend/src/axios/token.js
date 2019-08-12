/**
 * Manage the how Access Tokens are being stored and retreived from storage.
 */

const TOKEN = 'token';
const TOKEN_KEY = 'tokenKey';

const TokenService = {
  getToken() {
    return sessionStorage.getItem(TOKEN) || '';
  },

  getTokenKey() {
    return sessionStorage.getItem(TOKEN_KEY) || '';
  },

  saveToken(token) {
    sessionStorage.setItem(TOKEN, token);
  },

  saveTokenKey(tokenKey) {
    sessionStorage.setItem(TOKEN_KEY, tokenKey);
  },

  removeToken() {
    sessionStorage.removeItem(TOKEN);
  },

  removeTokenKey() {
    sessionStorage.removeItem(TOKEN_KEY);
  },
};

export default TokenService;
