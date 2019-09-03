import axios from 'axios';
import { authCallback, authAPI } from '@/app.config';

export const getToken = async (authCode, clientId) => {
  const headers = {
    'Content-Type': 'application/x-www-form-urlencoded',
    auth: {
      username: clientId,
    },
  };
  // pass data to type x-www-form-urlendcoded
  const params = new URLSearchParams();
  params.append('code', authCode);
  params.append('grant_type', 'authorization_code');
  // eslint-disable-next-line no-undef
  params.append('redirect_uri', authCallback);

  // eslint-disable-next-line camelcase
  const { access_token } = await axios.post(
    // eslint-disable-next-line no-undef
    `${authAPI}/token`,
    params,
    headers,
  );
  // eslint-disable-next-line camelcase
  return access_token;
};

export function downLoadFile(filename, file) {
  const element = document.createElement('a');
  element.href = file;
  element.download = filename;
  element.target = '_blank';
  document.body.appendChild(element);
  element.click();
  document.body.removeChild(element);
}
export default {
  downLoadFile,
  getToken,
};
