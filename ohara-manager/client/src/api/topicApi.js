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

import { toNumber, get } from 'lodash';

import { handleError, axiosInstance } from './apiUtils';

export const fetchTopic = async name => {
  try {
    const res = await axiosInstance.get(`/api/topics/${name}`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const fetchTopics = async () => {
  try {
    const res = await axiosInstance.get('/api/topics');
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createTopic = async params => {
  const { name, numberOfPartitions, numberOfReplications } = params;

  try {
    const data = {
      name,
      numberOfPartitions: toNumber(numberOfPartitions),
      brokerClusterName: params.brokerClusterName,
      numberOfReplications: toNumber(numberOfReplications),
    };

    const res = await axiosInstance.post('/api/topics', data);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const startTopic = async name => {
  try {
    const res = await axiosInstance.put(`/api/topics/${name}/start`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const stopTopic = async name => {
  try {
    const res = await axiosInstance.put(`/api/topics/${name}/stop`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteTopic = async name => {
  try {
    const res = await axiosInstance.delete(`/api/topics/${name}`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
