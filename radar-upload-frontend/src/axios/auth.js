import axios from 'axios';

export default {
  logout() {
    return axios.get('/logout');
  },
  login() {
    return axios.get('/login');
  },
};
