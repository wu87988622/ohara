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

import * as log from './body/logBody';
import { responseUtil, axiosInstance } from './utils/apiUtils';
import * as URL from './utils/url';

const url = URL.LOG_URL;

export const services = {
  configurator: 'configurator',
  zookeeper: 'zookeepers',
  broker: 'brokers',
  worker: 'workers',
  stream: 'streams',
};

export const getConfiguratorLog = async (params = {}) => {
  const res = await axiosInstance.get(
    `${url}/${services.configurator}${URL.toQueryParameters(params)}`,
  );
  return responseUtil(res, log);
};

export const getZookeeperLog = async params => {
  const { name, group } = params;
  const res = await axiosInstance.get(
    `${url}/${services.zookeeper}/${name}?group=${group}`,
  );
  return responseUtil(res, log);
};

export const getBrokerLog = async params => {
  const { name, group } = params;
  const res = await axiosInstance.get(
    `${url}/${services.broker}/${name}?group=${group}`,
  );
  return responseUtil(res, log);
};

export const getWorkerLog = async params => {
  const { name, group } = params;
  const res = await axiosInstance.get(
    `${url}/${services.worker}/${name}?group=${group}`,
  );
  return responseUtil(res, log);
};

export const getStreamLog = async params => {
  const { name, group } = params;
  const res = await axiosInstance.get(
    `${url}/${services.stream}/${name}?group=${group}`,
  );
  return responseUtil(res, log);
};
