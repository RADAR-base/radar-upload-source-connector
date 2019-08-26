import axios from 'axios';

const getToken = async (authCode, clientId = 'radar_upload_frontend') => {
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
  params.append('redirect_uri', 'http://localhost:8080/login');

  // eslint-disable-next-line camelcase
  const { access_token } = await axios.post(
    'https://radar-test.thehyve.net/managementportal/oauth/token',
    params,
    headers,
  );
  // eslint-disable-next-line camelcase
  return access_token;
};

export const getAuth = async () => {
  // const currentToken = localStorage.getItem('token');
  const isRedirectUrl = window.location.pathname.includes('/login');
  if (!isRedirectUrl) {
    localStorage.removeItem('token');
    window.open('https://radar-test.thehyve.net/managementportal/oauth/authorize?client_id=radar_upload_frontend&response_type=code&redirect_uri=http://localhost:8080/login');
    const checkToken = setInterval(() => {
      if (localStorage.getItem('token')) {
        clearInterval(checkToken);
        window.location.reload();
      }
    }, 500);
    return;
  }
  if (isRedirectUrl) {
    const authCode = window.location.search.replace('?code=', '');
    const returnedToken = await getToken(authCode).catch(() => null);
    localStorage.setItem('token', returnedToken);
    window.close();
  }
};
function downLoadFile(filename, text) {
  const element = document.createElement('a');
  element.setAttribute('href', `data:text/plain;charset=utf-8,${encodeURIComponent(text)}`);
  element.setAttribute('download', filename);
  element.style.display = 'none';
  document.body.appendChild(element);
  element.click();
  document.body.removeChild(element);
}
export default {
  getAuth,
  downLoadFile,
};
