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

import { get, size } from 'lodash';

import { handleError, axiosInstance } from './apiUtils';

export const fetchProperty = async name => {
  try {
    const res = await axiosInstance.get(`/api/stream/${name}`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createProperty = async params => {
  try {
    const res = await axiosInstance.post('/api/stream', params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const updateProperty = async params => {
  try {
    const propertyName = params.name;
    const url = `/api/stream/${propertyName}`;
    const from = size(params.from) > 0 ? params.from : [];
    const to = size(params.to) > 0 ? params.to : [];

    const data = {
      ...params,
      from,
      to,
      instances: params.instances ? Number(params.instances) : 1,
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

export const deleteProperty = async name => {
  try {
    const res = await axiosInstance.delete(`/api/stream/${name}`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const startStreamApp = async name => {
  try {
    const res = await axiosInstance.put(`/api/stream/${name}/start`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const stopStreamApp = async name => {
  try {
    const res = await axiosInstance.put(`/api/stream/${name}/stop`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
