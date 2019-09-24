/* eslint-disable no-undef */
import axios from 'axios';
import auth from '../auth';

jest.mock('axios');

describe('axios/projects', () => {
  it('login', async () => {
    axios.get.mockResolvedValue('');
    await auth.login();
    expect(axios.get).toBeCalledWith('/login');
    axios.get.mockClear();
  });

  it('logout', async () => {
    axios.get.mockResolvedValue('');
    await auth.logout();
    expect(axios.get).toBeCalledWith('/logout');
  });
});
