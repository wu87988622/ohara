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

import { isEmpty } from 'lodash';
import * as inspectConfiguratorBody from './body/inspectConfiguratorBody';
import * as inspectManagerBody from './body/inspectManagerBody';
import * as inspectServiceBody from './body/inspectServiceBody';
import * as inspectTopicBody from './body/inspectTopicBody';
import * as inspectRdbBody from './body/inspectRdbBody';
import * as file from './body/fileBody';
import {
  getKey,
  requestUtil,
  responseUtil,
  axiosInstance,
} from './utils/apiUtils';
import * as URL from './utils/url';

const url = URL.INSPECT_URL;
export const kind = {
  configurator: 'configurator',
  zookeeper: 'zookeeper',
  broker: 'broker',
  worker: 'worker',
  stream: 'stream',
  manager: 'manager',
  rdb: 'rdb',
  topic: 'topic',
  file: 'files',
};

export const classType = {
  stream: 'stream',
  sink: 'sink',
  source: 'source',
};

const fetchServiceInfo = async (kind, params) => {
  const reqUrl = !isEmpty(params)
    ? `${url}/${kind}/${params.name}?group=${params.group}`
    : `${url}/${kind}`;
  const res = await axiosInstance.get(reqUrl);
  const result = responseUtil(res, inspectServiceBody);
  result.title =
    `Inspect ${kind} ${getKey(params)} info ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const getConfiguratorInfo = async () => {
  const res = await axiosInstance.get(`${url}/${kind.configurator}`);
  const result = responseUtil(res, inspectConfiguratorBody);
  result.title =
    `Inspect ${kind.configurator} info ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const getZookeeperInfo = async (params = {}) => {
  return fetchServiceInfo(kind.zookeeper, params);
};

export const getBrokerInfo = async (params = {}) => {
  return fetchServiceInfo(kind.broker, params);
};

export const getWorkerInfo = async (params = {}) => {
  return fetchServiceInfo(kind.worker, params);
};

export const getStreamsInfo = async (params = {}) => {
  return fetchServiceInfo(kind.stream, params);
};

export const getManagerInfo = async () => {
  const res = await axiosInstance.get(`${url}/${kind.manager}`);
  const result = responseUtil(res, inspectManagerBody);
  result.title =
    `Inspect ${kind.manager} info ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const getFileInfoWithoutUpload = async params => {
  const fileUrl = `${url}/${kind.file}`;

  const requestBody = requestUtil(params, file);
  const config = {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  };

  let formData = new FormData();
  formData.append('file', requestBody.file);
  formData.append('group', requestBody.group);
  if (requestBody.tags) {
    formData.append('tags', JSON.stringify(requestBody.tags));
  }
  const res = await axiosInstance.post(fileUrl, formData, config);
  const result = responseUtil(res, file);
  result.title =
    `Inspect ${kind.file} ${getKey(params)} info ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const getTopicData = async params => {
  const { name, ...others } = params;
  const topicUrl =
    `${url}/${kind.topic}/${name}` + URL.toQueryParameters(others);
  const res = await axiosInstance.post(topicUrl);
  const result = responseUtil(res, inspectTopicBody);
  result.title =
    `Inspect ${kind.topic} ${getKey(params)} info ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const getRdbData = async params => {
  const rdbUrl = `${url}/${kind.rdb}`;
  const requestBody = requestUtil(params, inspectRdbBody);
  const res = await axiosInstance.post(rdbUrl, requestBody);
  const result = responseUtil(res, inspectRdbBody);
  result.title =
    `Inspect ${kind.rdb} info ` + (result.errors ? 'failed.' : 'successful.');
  return result;
};
