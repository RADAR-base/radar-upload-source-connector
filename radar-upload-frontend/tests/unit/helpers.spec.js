/* eslint-disable camelcase */
// test application helpers functions

import axios from 'axios';
import { getToken, downLoadFile } from '@/helpers.js';

jest.mock('axios');


describe('helpers function', () => {
  it('getToken', async () => {
    const authCode = 'authCode';
    const clientId = 'clientId';
    const postReturnValue = { access_token: 'here is access token' };
    axios.post.mockResolvedValue(postReturnValue);
    const params = new URLSearchParams();
    params.append('code', authCode);
    params.append('grant_type', 'authorization_code');
    params.append('redirect_uri', 'http://localhost:8080/login');

    const headers = {
      'Content-Type': 'application/x-www-form-urlencoded',
      auth: {
        username: clientId,
      },
    };

    const token = await getToken(authCode, clientId);
    expect(axios.post)
      .toBeCalledWith('https://radar-test.thehyve.net/managementportal/oauth/token',
        params, headers);
    expect(token).toEqual(postReturnValue.access_token);
  });
});
