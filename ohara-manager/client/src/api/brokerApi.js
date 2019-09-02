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

export const fetchBrokers = async () => {
  try {
    const res = await axiosInstance.get(`/api/brokers`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const fetchBroker = async brokerName => {
  try {
    const res = await axiosInstance.get(`/api/brokers/${brokerName}`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createBroker = async params => {
  try {
    const url = `/api/brokers`;
    const data = {
      name: params.name,
      zookeeperClusterName: params.zookeeperClusterName,
      nodeNames: params.nodeNames || [],
      clientPort: params.clientPort,
      exporterPort: params.exporterPort,
      jmxPort: params.jmxPort,
      tags: params.tags,
    };
    const config = {
      timeout: 3 * 60 * 1000, // set timeout to 3 minutes.
    };

    const res = await axiosInstance.post(url, data, config);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const addNodeToBroker = async params => {
  try {
    const { name, nodeName } = params;
    const url = `/api/brokers/${name}/${nodeName}`;
    const res = await axiosInstance.put(url);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const startBroker = async name => {
  try {
    const url = `/api/brokers/${name}/start`;

    const res = await axiosInstance.put(url);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const stopBroker = async name => {
  try {
    const url = `/api/brokers/${name}/stop`;

    const res = await axiosInstance.put(url);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteBroker = async name => {
  try {
    const url = `/api/brokers/${name}`;
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
