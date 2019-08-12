import axios from 'axios';
import TokenService from './token';

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

  setHeader() {
    axios.defaults.headers.common.Authorization = `Bearer ${TokenService.getToken()}`;
  },

  removeHeader() {
    axios.defaults.headers.common = {};
  },

  setLanguage(language) {
    axios.defaults.headers.common['Accept-Language'] = language;
  },

  // get(resource) {
  //   return axios.get(resource);
  // },

  // post(resource, data) {
  //   return axios.post(resource, data);
  // },

  // put(resource, data) {
  //   return axios.put(resource, data);
  // },

  // delete(resource) {
  //   return axios.delete(resource);
  // },

  // all(resource) {
  //   return axios.all(resource.map(element => ApiService.get(element)));
  // },
  // /**
  //  * Perform a custom Axios request.
  //  *
  //  * @param {Object} data
  //  * @param {String} data.method
  //  * @param {String} data.baseURL
  //  * @param {String} data.data - Request payload
  //  * @param [Object] data.auth
  //  * @returns
  //  */
  // customRequest(data) {
  //   return axios(data);
  // },
};

export default ApiService;
