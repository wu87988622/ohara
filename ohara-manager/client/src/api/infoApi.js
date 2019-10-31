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

import * as infoConfiguratorBody from './body/infoConfiguratorBody';
import * as infoServiceBody from './body/infoServiceBody';
import { responseUtil, axiosInstance } from './utils/apiUtils';
import * as URL from './utils/url';

const url = URL.INFO_URL;
export const service = {
  configurator: 'configurator',
  zookeeper: 'zookeeper',
  broker: 'broker',
  worker: 'worker',
};

export const getConfiguratorInfo = async (params = {}) => {
  const res = await axiosInstance.get(
    url + '/' + service.configurator + URL.toQueryParameters(params),
  );
  return responseUtil(res, infoConfiguratorBody);
};

export const getZookeeperInfo = async (params = {}) => {
  const res = await axiosInstance.get(
    url + '/' + service.zookeeper + URL.toQueryParameters(params),
  );
  return responseUtil(res, infoServiceBody);
};

export const getBrokerInfo = async (params = {}) => {
  const res = await axiosInstance.get(
    url + '/' + service.broker + URL.toQueryParameters(params),
  );
  return responseUtil(res, infoServiceBody);
};

export const getWorkerInfo = async (params = {}) => {
  const res = await axiosInstance.get(
    url + '/' + service.worker + URL.toQueryParameters(params),
  );
  return responseUtil(res, infoServiceBody);
};
