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

import { useState, useEffect, useCallback } from 'react';
import useSnackbar from 'components/context/Snackbar/useSnackbar';
import { get } from 'lodash';
import { handleError, axiosInstance } from './apiUtils';

export const useFetchApi = (url, name = '') => {
  const { showMessage } = useSnackbar();
  const [response, setResponse] = useState();
  const [isLoading, setIsloading] = useState(false);
  const [refetch, setRefetch] = useState('');
  const request = useCallback(
    async url => {
      try {
        setIsloading(true);
        const newUrl = name === '' ? url : `${url}/${name}`;
        const res = await axiosInstance.get(newUrl);
        const isSuccess = get(res, 'data.isSuccess', false);

        if (!isSuccess) {
          showMessage(handleError(res));
        }
        setResponse(res);
        setIsloading(false);
        setRefetch('');
      } catch (err) {
        showMessage(handleError(err));
      }
    },
    [name, showMessage],
  );

  useEffect(() => {
    request(url, name);
  }, [name, request, url, refetch]);

  return { data: response, isLoading, setRefetch };
};
