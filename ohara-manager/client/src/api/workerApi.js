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
import * as worker from './body/workerBody';
import {
  getKey,
  requestUtil,
  responseUtil,
  axiosInstance,
} from './utils/apiUtils';
import * as URL from './utils/url';
import wait from './waitApi';
import * as waitUtil from './utils/waitUtils';
import * as inspect from './inspectApi';

const url = URL.WORKER_URL;

export const create = async (params, body = {}) => {
  if (isEmpty(body)) {
    const info = await inspect.getWorkerInfo();
    if (!info.errors) body = info.data;
  }

  const requestBody = requestUtil(params, worker, body);
  const res = await axiosInstance.post(url, requestBody);
  const result = responseUtil(res, worker);
  result.title =
    `Create worker ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const start = async params => {
  const { name, group } = params;
  await axiosInstance.put(`${url}/${name}/start?group=${group}`);
  await wait({
    url: `${URL.INSPECT_URL}/${inspect.kind.worker}/${name}?group=${group}`,
    checkFn: waitUtil.waitForConnectReady,
  });
  const res = await axiosInstance.get(`${url}/${name}?group=${group}`);
  const result = responseUtil(res, worker);
  result.title =
    `Start worker ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const update = async params => {
  const { name, group } = params;
  delete params[name];
  delete params[group];
  const body = params;
  const res = await axiosInstance.put(`${url}/${name}?group=${group}`, body);
  const result = responseUtil(res, worker);
  result.title =
    `Update worker ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const stop = async params => {
  const { name, group } = params;
  await axiosInstance.put(`${url}/${name}/stop?group=${group}`);
  const res = await wait({
    url: `${url}/${name}?group=${group}`,
    checkFn: waitUtil.waitForStop,
  });
  const result = responseUtil(res, worker);
  result.title =
    `Stop worker ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const remove = async params => {
  const { name, group } = params;
  await axiosInstance.delete(`${url}/${name}?group=${group}`);
  const res = await wait({
    url,
    checkFn: waitUtil.waitForClusterNonexistent,
    paramRes: params,
  });
  const result = responseUtil(res, worker);
  result.title =
    `Remove worker ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const get = async params => {
  const { name, group } = params;
  const res = await axiosInstance.get(`${url}/${name}?group=${group}`);
  const result = responseUtil(res, worker);
  result.title =
    `Get worker ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const getAll = async (params = {}) => {
  const res = await axiosInstance.get(url + URL.toQueryParameters(params));
  const result = responseUtil(res, worker);
  result.title =
    `Get worker list ` + (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const addNode = async params => {
  const { name, group, nodeName } = params;
  await axiosInstance.put(`${url}/${name}/${nodeName}?group=${group}`);
  const res = await wait({
    url: `${url}/${name}?group=${group}`,
    checkFn: waitUtil.waitForNodeReady,
    paramRes: nodeName,
  });
  const result = responseUtil(res, worker);
  result.title =
    `Add node to worker ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const removeNode = async params => {
  const { name, group, nodeName } = params;
  await axiosInstance.delete(`${url}/${name}/${nodeName}?group=${group}`);
  const res = await wait({
    url: `${url}/${name}?group=${group}`,
    checkFn: waitUtil.waitForNodeNonexistentInCluster,
    paramRes: nodeName,
  });
  const result = responseUtil(res, worker);
  result.title =
    `Remove node from worker ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};
