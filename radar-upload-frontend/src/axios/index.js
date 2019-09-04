/* eslint-disable consistent-return */
import axios from 'axios';

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
            localStorage.removeItem('token');
            // eslint-disable-next-line no-case-declarations
            router.replace('/login');
            store.commit('openSnackbar', { type: 'error', text: 'Please login to continue' });
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
