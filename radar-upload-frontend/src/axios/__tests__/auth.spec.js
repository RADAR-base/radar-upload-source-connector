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
  it('fetchToken', async () => {
    const authCode = 'authCode';
    const clientId = 'clientId';
    const postReturnValue = { access_token: 'here is access token', expires_in: 3600 };
    const callback = 'http://localhost:8080/login';
    axios.post.mockResolvedValue(postReturnValue);
    const params = new URLSearchParams();
    params.append('code', authCode);
    params.append('grant_type', 'authorization_code');
    params.append('redirect_uri', callback);

    const config = {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      auth: {
        username: clientId,
        password: ''
      },
    };

    const now = new Date().getTime();
    const token = await auth.fetchToken(authCode, { clientId, authCallback: callback });
    expect(axios.post)
            .toBeCalledWith('https://radar-test.thehyve.net/managementportal/oauth/token',
                    params, config);
    expect(token.token).toEqual(postReturnValue.access_token);
    expect(token.expirationDate.getTime()).toBeGreaterThanOrEqual(now + postReturnValue.expires_in);
    expect(token.expirationDate.getTime()).toBeLessThanOrEqual(now + postReturnValue.expires_in + 5);
  });
});
