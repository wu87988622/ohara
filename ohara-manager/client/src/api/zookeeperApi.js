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
import * as zookeeper from './body/zookeeperBody';
import { requestUtil, responseUtil, axiosInstance } from './utils/apiUtils';
import * as URL from './utils/url';
import wait from './waitApi';
import * as waitUtil from './utils/waitUtils';
import * as inspect from './inspectApi';

const url = URL.ZOOKEEPER_URL;

export const create = async (params, body = {}) => {
  if (isEmpty(body)) {
    const info = await inspect.getZookeeperInfo();
    if (!info.errors) body = info.data;
  }

  const { name } = params;
  const requestBody = requestUtil(params, zookeeper, body);
  const res = await axiosInstance.post(url, requestBody);
  const result = responseUtil(res, zookeeper);
  result.title = result.errors
    ? `Failed to create zookeeper ${name}.`
    : `Successfully created zookeeper ${name}.`;
  return result;
};

export const start = async params => {
  const { name, group } = params;
  const startRes = await axiosInstance.put(
    `${url}/${name}/start?group=${group}`,
  );

  let result = {};
  if (startRes.data.isSuccess) {
    const res = await wait({
      url: `${url}/${name}?group=${group}`,
      checkFn: waitUtil.waitForRunning,
    });
    result = responseUtil(res, zookeeper);
  } else {
    result = responseUtil(startRes, zookeeper);
  }
  result.title = result.errors
    ? `Failed to start zookeeper ${name}.`
    : `Successfully started zookeeper ${name}.`;
  return result;
};

export const update = async params => {
  const { name, group } = params;
  delete params[name];
  delete params[group];
  const body = params;
  const res = await axiosInstance.put(`${url}/${name}?group=${group}`, body);
  const result = responseUtil(res, zookeeper);
  result.title = result.errors
    ? `Failed to update zookeeper ${name}.`
    : `Successfully updated zookeeper ${name}.`;
  return result;
};

export const forceStop = async params => {
  const { name, group } = params;
  const stopRes = await axiosInstance.put(
    `${url}/${name}/stop?group=${group}&force=true`,
  );

  let result = {};
  if (stopRes.data.isSuccess) {
    const res = await wait({
      url: `${url}/${name}?group=${group}`,
      checkFn: waitUtil.waitForStop,
    });
    result = responseUtil(res, zookeeper);
  } else {
    result = responseUtil(stopRes, zookeeper);
  }
  result.title = result.errors
    ? `Failed to stop zookeeper ${name}.`
    : `Successfully stopped zookeeper ${name}.`;
  return result;
};

export const stop = async params => {
  const { name, group } = params;
  const stopRes = await axiosInstance.put(`${url}/${name}/stop?group=${group}`);

  let result = {};
  if (stopRes.data.isSuccess) {
    const res = await wait({
      url: `${url}/${name}?group=${group}`,
      checkFn: waitUtil.waitForStop,
    });
    result = responseUtil(res, zookeeper);
  } else {
    result = responseUtil(stopRes, zookeeper);
  }
  result.title = result.errors
    ? `Failed to stop zookeeper ${name}.`
    : `Successfully stopped zookeeper ${name}.`;
  return result;
};

export const remove = async params => {
  const { name, group } = params;
  const deletedRes = await axiosInstance.delete(
    `${url}/${name}?group=${group}`,
  );

  let result = {};
  if (deletedRes.data.isSuccess) {
    const res = await wait({
      url,
      checkFn: waitUtil.waitForClusterNonexistent,
      paramRes: params,
    });
    result = responseUtil(res, zookeeper);
  } else {
    result = responseUtil(deletedRes, zookeeper);
  }
  result.title = result.errors
    ? `Failed to remove zookeeper ${name}.`
    : `Successfully removed zookeeper ${name}.`;
  return result;
};

export const get = async params => {
  const { name, group } = params;
  const res = await axiosInstance.get(`${url}/${name}?group=${group}`);
  const result = responseUtil(res, zookeeper);
  result.title = result.errors
    ? `Failed to get zookeeper ${name}.`
    : `Successfully got zookeeper ${name}.`;
  return result;
};

export const getAll = async (params = {}) => {
  const res = await axiosInstance.get(url + URL.toQueryParameters(params));
  const result = responseUtil(res, zookeeper);
  result.title = result.errors
    ? `Failed to get zookeeper list.`
    : `Successfully got zookeeper list.`;
  return result;
};
