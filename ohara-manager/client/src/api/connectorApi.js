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

export const fetchConnector = async (group, name) => {
  try {
    const res = await axiosInstance.get(
      `/api/connectors/${name}?group=${group}`,
    );
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createConnector = async params => {
  try {
    const res = await axiosInstance.post('/api/connectors', params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const updateConnector = async ({ name, group, params }) => {
  try {
    const res = await axiosInstance.put(
      `/api/connectors/${name}?group=${group}`,
      params,
    );
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const startConnector = async (group, name) => {
  try {
    const res = await axiosInstance.put(
      `/api/connectors/${name}/start?group=${group}`,
    );
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const stopConnector = async (group, name) => {
  try {
    const res = await axiosInstance.put(
      `/api/connectors/${name}/stop?group=${group}`,
    );
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteConnector = async (group, name) => {
  try {
    const res = await axiosInstance.delete(
      `/api/connectors/${name}?group=${group}`,
    );
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
