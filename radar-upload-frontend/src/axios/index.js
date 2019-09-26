/* eslint-disable consistent-return */
import axios from 'axios';
import auth from './auth';

const ApiService = {
  authInit(store, router) {
    const loginIndex = window.location.href.indexOf('/login?code=');

    if (loginIndex > 0 && window.location.href[loginIndex - 1] !== '#') {
      const loginRoute = `${window.location.href.substring(0, loginIndex + 1)}#/login${window.location.search}`;
      console.log('Let login component handle login code', loginRoute);
      window.location.href = loginRoute;
      return false;
    }

    const currentToken = auth.getToken();
    if (currentToken == null) {
      auth.authorizationFailed(store, router);
      return true;
    }
    console.log('Updating authorization');
    axios.defaults.headers.common.Authorization = `Bearer ${currentToken}`;
    return true;
  },
  axiosInit(baseURL, store, router) {
    axios.defaults.baseURL = baseURL;
    axios.interceptors.response.use(
      response => response.data,
      // eslint-disable-next-line func-names
      async (error) => {
        if (error.response.status === 401) {
          auth.authorizationFailed(store, router);
        } else {
          throw error.response;
        }
      },
    );
  },
  setLanguage(language) {
    axios.defaults.headers.common['Accept-Language'] = language;
  },
};

export default ApiService;
