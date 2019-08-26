import axios from 'axios';

export default {
  getProjects() {
    return axios.get('/projects')
      .then(res => res.projects.map(el => ({
        text: el.name || el.id,
        value: el.id,
        ...el,
      })));
  },
};
