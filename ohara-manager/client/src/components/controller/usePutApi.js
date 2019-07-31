import { useRef } from 'react';
import { get } from 'lodash';

import useSnackbar from 'components/context/Snackbar/useSnackbar';
import { handleError, axiosInstance } from './apiUtils';

const usePutApi = url => {
  const { showMessage } = useSnackbar();
  const resData = useRef();

  const request = async (type, data) => {
    try {
      const res = await axiosInstance.put(`${url}${type}`, data);
      const isSuccess = get(res, 'data.isSuccess', false);

      if (!isSuccess) {
        showMessage(handleError(res));
      }
      resData.current = res;
    } catch (err) {
      showMessage(handleError(err));
    }
  };

  const putApi = async (type, data) => {
    await request(type, data);
  };

  const getData = () => resData.current;

  return { getData, putApi };
};

export default usePutApi;
