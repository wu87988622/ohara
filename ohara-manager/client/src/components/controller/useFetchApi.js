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
import { get } from 'lodash';

import useSnackbar from 'components/context/Snackbar/useSnackbar';
import { handleError, axiosInstance } from './apiUtils';

const useFetchApi = url => {
  const { showMessage } = useSnackbar();
  const [response, setResponse] = useState();
  const [isLoading, setIsLoading] = useState(true);
  const [refetchState, refetch] = useState(null);
  const request = useCallback(
    async url => {
      try {
        const res = await axiosInstance.get(url);
        const isSuccess = get(res, 'data.isSuccess', false);

        if (!isSuccess) {
          showMessage(handleError(res));
        }
        setResponse(res);
        setIsLoading(false);
        refetch(null);
      } catch (err) {
        showMessage(handleError(err));
      }
    },
    [showMessage],
  );

  useEffect(() => {
    request(url);
  }, [request, url, refetchState]);

  return { data: response, isLoading, refetch };
};

export default useFetchApi;
