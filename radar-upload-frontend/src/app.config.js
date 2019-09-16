/* eslint-disable no-undef */

export const clientId = process.env.VUE_APP_CLIENT_ID || $VUE_APP_CLIENT_ID;
export const authAPI = process.env.VUE_APP_AUTH_API || $VUE_APP_AUTH_API;
export const authCallback = process.env.VUE_APP_AUTH_CALLBACK || $VUE_APP_AUTH_CALLBACK;
export const baseURL = process.env.VUE_APP_API_BASE_URL || $VUE_APP_API_BASE_URL;

export default {
  clientId,
  authAPI,
  authCallback,
  baseURL,
};
