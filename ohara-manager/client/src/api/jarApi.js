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

import { get, isUndefined } from 'lodash';

import { handleError, axiosInstance } from './apiUtils';

export const fetchJars = async group => {
  try {
    const res = await axiosInstance.get(`/api/files?group=${group}`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createJar = async params => {
  try {
    const { file, workerClusterName } = params;
    const url = `/api/files`;
    const formData = new FormData();
    formData.append('file', file);
    if (!isUndefined(workerClusterName)) {
      formData.append('group', workerClusterName);
    }
    const config = {
      headers: {
        'content-type': 'multipart/form-data',
      },
    };
    const res = await axiosInstance.post(url, formData, config);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteJar = async params => {
  try {
    const { name, workerClusterName } = params;
    const url = `/api/files/${name}?group=${workerClusterName}`;
    const res = await axiosInstance.delete(url);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
