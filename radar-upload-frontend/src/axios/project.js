import axios from 'axios';

export default {
  async getProjects() {
    const result = await axios.get('projects');
    return result.projects.map(el => ({
      text: el.name || el.id,
      value: el.id,
      ...el,
    }));
  },
};
