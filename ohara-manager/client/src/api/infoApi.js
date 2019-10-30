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

import * as info from './body/infoBody';
import { responseUtil, axiosInstance } from './utils/apiUtils';
import * as URL from './utils/url';

const url = URL.INFO_URL;
const service = {
  zookeeper: 'zookeeper',
  broker: 'broker',
  worker: 'worker',
};

export const getConfiguratorInfo = async (params = {}) => {
  const parameter = Object.keys(params).map(key => `?${key}=${params[key]}&`);
  const res = await axiosInstance.get(url + '/configurator' + parameter);
  return responseUtil(res, info);
};

export const getZookeeperInfo = async (params = {}) => {
  const parameter = Object.keys(params).map(key => `?${key}=${params[key]}&`);
  const res = await axiosInstance.get(
    url + '/' + service.zookeeper + parameter,
  );
  return responseUtil(res, info);
};

export const getBrokerInfo = async (params = {}) => {
  const parameter = Object.keys(params).map(key => `?${key}=${params[key]}&`);
  const res = await axiosInstance.get(url + '/' + service.broker + parameter);
  return responseUtil(res, info);
};

export const getWorkerInfo = async (params = {}) => {
  const parameter = Object.keys(params).map(key => `?${key}=${params[key]}&`);
  const res = await axiosInstance.get(url + '/' + service.worker + parameter);
  return responseUtil(res, info);
};
