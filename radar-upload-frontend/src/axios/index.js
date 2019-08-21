import axios from 'axios';

const ApiService = {
  init(baseURL, store, router) {
    axios.defaults.baseURL = baseURL;
    axios.interceptors.response.use(
      response =>
        // Do something with response data
        // eslint-disable-next-line
        response.data,
      (error) => {
        // Do something with response error
        // store.commit('removeLoader');
        // console.log(error.response);
        switch (error.response.status) {
          case 401:
            router.replace('/');
            break;
          default:
            break;
        }
        return Promise.reject(error.response);
      },
    );
  },

  setHeader(token) {
    axios.defaults.headers.common.Authorization = `Bearer ${token}`;
  },

  removeHeader() {
    axios.defaults.headers.common = {};
  },

  setLanguage(language) {
    axios.defaults.headers.common['Accept-Language'] = language;
  },

};

export default ApiService;
