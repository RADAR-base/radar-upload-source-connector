/* eslint-disable no-undef */
import axios from 'axios';
import projectAPI from '../project';

jest.mock('axios');

describe('axios/projects', () => {
  it('return correct projects payload', () => {
    const projects = [{ text: '', value: '' }].map(el => ({
      text: el.name || el.id,
      value: el.id,
      ...el,
    }));
    const resp = { projects };
    axios.get.mockResolvedValue(resp);

    return projectAPI.getProjects().then((data) => {
      expect(data).toEqual(projects);
    });
  });
});
