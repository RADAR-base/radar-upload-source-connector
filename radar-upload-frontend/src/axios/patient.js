import axios from 'axios';

export default {
  filteredPatients(projectId) {
    return axios.get(`/projects/${projectId}/users`)
      .then(res => res.users.map((el, index) => ({
        sequence: index + 1,
        patientName: el.externalId,
        status: el.status,
        patientId: el.id,
      })));
  },
};
