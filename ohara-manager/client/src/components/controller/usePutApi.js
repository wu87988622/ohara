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
