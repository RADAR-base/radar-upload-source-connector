import axios from 'axios';

export default {
  filteredPatients(projectId) {
    return axios.get(`/projects/${projectId}/users`);
  },
};
