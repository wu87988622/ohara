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

import { get } from 'lodash';

import { handleError, axiosInstance } from './apiUtils';

export const fetchNodes = async () => {
  try {
    const res = await axiosInstance.get(`/api/nodes`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const fetchNode = async params => {
  try {
    const res = await axiosInstance.get(`/api/nodes/${params.hostname}`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createNode = async params => {
  try {
    const url = `/api/nodes`;
    const data = {
      hostname: params.hostname,
      port: params.port,
      user: params.user,
      password: params.password,
    };

    const res = await axiosInstance.post(url, data);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const updateNode = async params => {
  try {
    const url = `/api/nodes/${params.hostname}`;
    const data = {
      hostname: params.hostname,
      port: params.port,
      user: params.user,
      password: params.password,
      tags: params.tags,
    };

    const res = await axiosInstance.put(url, data);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteNode = async params => {
  try {
    const url = `/api/nodes/${params.hostname}`;

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
