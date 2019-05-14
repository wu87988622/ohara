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

export const fetchJar = async workerClusterName => {
  try {
    const res = await axiosInstance.get(
      `/api/stream/jars?cluster=${workerClusterName}`,
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

export const uploadJar = async params => {
  try {
    const { workerClusterName, file } = params;
    const url = `/api/stream/jars`;
    const formData = new FormData();

    formData.append('streamapp', file);
    formData.append('cluster', workerClusterName);

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
    const { id } = params;
    const url = `/api/stream/jars/${id}`;
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

export const updateJarName = async params => {
  try {
    const { id, jarName } = params;
    const url = `/api/stream/jars/${id}`;
    const data = {
      jarName,
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

export const fetchProperty = async id => {
  try {
    const res = await axiosInstance.get(`/api/stream/property/${id}`);
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
    const streamAppId = params.id;
    const url = `/api/stream/property/${streamAppId}`;
    const data = {
      name: params.name,
      from: params.from || [],
      to: params.to || [],
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

export const startStreamApp = async id => {
  try {
    const res = await axiosInstance.put(`/api/stream/${id}/start`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const stopStreamApp = async id => {
  try {
    const res = await axiosInstance.put(`/api/stream/${id}/stop`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteProperty = async id => {
  try {
    const res = await axiosInstance.delete(`/api/stream/property/${id}`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
