import axios from 'axios';
import humps from 'humps';

function createAxios() {
  const instance = axios.create({
    validateStatus: status => status >= 200 && status < 300,
  });

  // Add a request interceptor
  instance.interceptors.request.use(config => {
    let headers = config.headers;
    if (
      config.method === 'post' ||
      config.method === 'put' ||
      config.method === 'patch'
    ) {
      if (headers['Content-Type'] === undefined) {
        headers = {
          ...headers,
          'Content-Type': 'application/json',
        };
      }
    }

    return {
      ...config,
      headers,
    };
  });

  // Add a response interceptor
  instance.interceptors.response.use(
    response => {
      return {
        data: {
          result: humps.camelizeKeys(response.data),
          isSuccess: true,
        },
      };
    },
    error => {
      const { data: errorMessage } = error.response;
      return { errorMessage, isSuccess: false };
    },
  );

  return instance;
}

const axiosInstance = createAxios();

export default axiosInstance;