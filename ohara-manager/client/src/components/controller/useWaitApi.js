import { useRef } from 'react';

import { handleError, axiosInstance } from './apiUtils';
import * as commonUtils from 'utils/commonUtils';
import useSnackbar from 'components/context/Snackbar/useSnackbar';

const useWaitApi = () => {
  const { showMessage } = useSnackbar();
  const finish = useRef();
  const request = async params => {
    try {
      const {
        url,
        checkFn,
        retryCount = 0,
        maxRetry = 10,
        sleep = 2000,
      } = params;

      let res;
      try {
        res = await axiosInstance.get(url);
      } catch (err) {
        showMessage(handleError(err));
      }

      if (checkFn(res)) {
        finish.current = true;
        return;
      } else if (maxRetry <= retryCount) {
        showMessage(`Wait count is over to ${maxRetry}!!`);
        finish.current = false;
        return;
      }

      const newParams = {
        url,
        checkFn,
        retryCount: retryCount + 1,
        maxRetry,
        sleep,
      };
      await commonUtils.sleep(sleep);
      await request(newParams);
    } catch (err) {
      showMessage('Wait is failed');
      finish.current = false;
    }
  };

  const waitApi = async params => {
    await request(params);
  };

  const getFinish = () => finish.current;

  return { waitApi, getFinish };
};

export default useWaitApi;
