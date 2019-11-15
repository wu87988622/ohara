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

import * as inspectConfiguratorBody from './body/inspectConfiguratorBody';
import * as inspectServiceBody from './body/inspectServiceBody';
import * as inspectFileBody from './body/inspectFileBody';
import * as inspectTopicBody from './body/inspectTopicBody';
import * as inspectRdbBody from './body/inspectRdbBody';
import { requestUtil, responseUtil, axiosInstance } from './utils/apiUtils';
import * as URL from './utils/url';

const url = URL.INSPECT_URL;
export const kind = {
  configurator: 'configurator',
  zookeeper: 'zookeeper',
  broker: 'broker',
  worker: 'worker',
  stream: 'stream',
  rdb: 'rdb',
  topic: 'topic',
  file: 'file',
};

export const classType = {
  stream: 'streamApp',
  sink: 'sink connector',
  source: 'source connector',
};

export const getConfiguratorInfo = async () => {
  const res = await axiosInstance.get(url + '/' + kind.configurator);
  return responseUtil(res, inspectConfiguratorBody);
};

export const getZookeeperInfo = async () => {
  const res = await axiosInstance.get(url + '/' + kind.zookeeper);
  return responseUtil(res, inspectServiceBody);
};

export const getBrokerInfo = async () => {
  const res = await axiosInstance.get(url + '/' + kind.broker);
  return responseUtil(res, inspectServiceBody);
};

export const getWorkerInfo = async params => {
  const workerUrl = params
    ? `${url}/${kind.worker}/${params.name}?group=${params.group}`
    : `${url}/${kind.worker}`;
  const res = await axiosInstance.get(workerUrl);
  return responseUtil(res, inspectServiceBody);
};

export const getStreamsInfo = async (params = {}) => {
  const streamUrl = params
    ? `${url}/${kind.stream}/${params.name}?group=${params.group}`
    : `${url}/${kind.stream}`;
  const res = await axiosInstance.get(streamUrl);
  return responseUtil(res, inspectServiceBody);
};

export const getFileInfo = async params => {
  const fileUrl = `${url}/${kind.file}/${params.name}?group=${params.group}`;
  const res = await axiosInstance.get(fileUrl);
  return responseUtil(res, inspectFileBody);
};

export const getTopicData = async params => {
  const { name, ...others } = params;
  const topicUrl =
    `${url}/${kind.topic}/${name}` + URL.toQueryParameters(others);
  const res = await axiosInstance.post(topicUrl);
  return responseUtil(res, inspectTopicBody);
};

export const getRdbData = async params => {
  const rdbUrl = `${url}/${kind.rdb}`;
  const requestBody = requestUtil(params, inspectRdbBody);
  const res = await axiosInstance.post(rdbUrl, requestBody);
  return responseUtil(res, inspectRdbBody);
};
