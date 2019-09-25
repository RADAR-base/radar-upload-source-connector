/* eslint-disable no-undef */
import axios from 'axios';
import auth from '../auth';

jest.mock('axios');

describe('axios/projects', () => {
  it('login', async () => {

    const tokenVal = 'here is the token';
    const now = new Date(Date.now());
    const localStoreSetToken = jest.spyOn(sessionStorage.__proto__, 'setItem');
    const token = {token: tokenVal, expirationDate: now};
    auth.login(token);
    expect(localStoreSetToken).toBeCalledWith('token', tokenVal);
    expect(localStoreSetToken).toBeCalledWith('tokenExpiration', now.toISOString());

  });

  it('logout', async () => {
    const localStoreRemoveToken = jest.spyOn(sessionStorage.__proto__, 'removeItem');
    auth.logout();
    expect(localStoreRemoveToken).toBeCalledWith('token');
    expect(localStoreRemoveToken).toBeCalledWith('tokenExpiration');
  });

  it('getToken', async () => {
    const oldGetItem = sessionStorage.__proto__.getItem;
    sessionStorage.__proto__.getItem = jest.fn().mockReturnValueOnce(null);
    expect(auth.getToken()).toBeNull();

    sessionStorage.__proto__.getItem = jest.fn()
            .mockReturnValueOnce(new Date(Date.now() - 60).toISOString());
    expect(auth.getToken()).toBeNull();

    sessionStorage.__proto__.getItem = jest.fn()
            .mockReturnValueOnce(new Date(Date.now() + 60).toISOString())
            .mockReturnValueOnce('test');
    expect(auth.getToken()).toEqual('test');

    sessionStorage.__proto__.getItem = oldGetItem;
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
    const token = await auth.fetchToken(authCode, { clientId, authCallback: callback , authAPI: 'http://localhost:8090/managementportal/oauth' });
    expect(axios.post)
            .toBeCalledWith('http://localhost:8090/managementportal/oauth/token',
                    params, config);
    expect(token.token).toEqual(postReturnValue.access_token);
    expect(token.expirationDate.getTime()).toBeGreaterThanOrEqual(now + postReturnValue.expires_in);

  });
});
