/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

      await commonUtils.sleep(sleep);
      await request({ ...params, retryCount: retryCount + 1 });
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
