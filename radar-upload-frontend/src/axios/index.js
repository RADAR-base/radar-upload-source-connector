/* eslint-disable consistent-return */
import axios from 'axios';
import { getAuth } from '@/helpers.js';

const ApiService = {
  init(baseURL, store, router) {
    const currentToken = localStorage.getItem('token');
    axios.defaults.headers.common.Authorization = `Bearer ${currentToken}`;
    axios.defaults.baseURL = baseURL;
    axios.interceptors.response.use(
      response => response.data,
      // eslint-disable-next-line func-names
      async (error) => {
        switch (error.response.status) {
          case 401:
            // store.commit('openSnackbar', { type: 'error', text: 'Please login to continue' });
            await getAuth();
            // eslint-disable-next-line no-case-declarations
            return;
          default:
            break;
        }
        return Promise.reject(error.response);
      },

    );
  },
  setLanguage(language) {
    axios.defaults.headers.common['Accept-Language'] = language;
  },

};

export default ApiService;
