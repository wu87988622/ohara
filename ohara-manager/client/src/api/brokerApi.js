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
import * as broker from './body/brokerBody';
import { requestUtil, responseUtil, axiosInstance } from './utils/apiUtils';
import * as URL from './utils/url';
import wait from './waitApi';
import * as waitUtil from './utils/waitUtils';
import * as inspect from './inspectApi';

const url = URL.BROKER_URL;

export const create = async (params, body = {}) => {
  if (isEmpty(body)) {
    const info = await inspect.getBrokerInfo();
    if (!info.errors) body = info.data;
  }

  const { name } = params;
  const requestBody = requestUtil(params, broker, body);
  const res = await axiosInstance.post(url, requestBody);
  const result = responseUtil(res, broker);
  result.title = result.errors
    ? `Failed to create broker ${name}.`
    : `Successfully created broker ${name}.`;
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
    result = responseUtil(res, broker);
  } else {
    result = responseUtil(startRes, broker);
  }
  result.title = result.errors
    ? `Failed to start broker ${name}.`
    : `Successfully started broker ${name}.`;
  return result;
};

export const update = async params => {
  const { name, group } = params;
  delete params[name];
  delete params[group];
  const body = params;
  const res = await axiosInstance.put(`${url}/${name}?group=${group}`, body);
  const result = responseUtil(res, broker);
  result.title = result.errors
    ? `Failed to update broker ${name}.`
    : `Successfully updated broker ${name}.`;
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
    result = responseUtil(res, broker);
  } else {
    result = responseUtil(stopRes, broker);
  }
  result.title = result.errors
    ? `Failed to stop broker ${name}.`
    : `Successfully stopped broker ${name}.`;
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
    result = responseUtil(res, broker);
  } else {
    result = responseUtil(stopRes, broker);
  }
  result.title = result.errors
    ? `Failed to stop broker ${name}.`
    : `Successfully stopped broker ${name}.`;
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
    result = responseUtil(res, broker);
  } else {
    result = responseUtil(deletedRes, broker);
  }
  result.title = result.errors
    ? `Failed to remove broker ${name}.`
    : `Successfully removed broker ${name}.`;
  return result;
};

export const get = async params => {
  const { name, group } = params;
  const res = await axiosInstance.get(`${url}/${name}?group=${group}`);
  const result = responseUtil(res, broker);
  result.title = result.errors
    ? `Failed to get broker ${name}.`
    : `Successfully got broker ${name}.`;
  return result;
};

export const getAll = async (params = {}) => {
  const res = await axiosInstance.get(url + URL.toQueryParameters(params));
  const result = responseUtil(res, broker);
  result.title = result.errors
    ? `Failed to get broker list.`
    : `Successfully got broker list.`;
  return result;
};

export const addNode = async params => {
  const { name, group, nodeName } = params;
  const addNodeRes = await axiosInstance.put(
    `${url}/${name}/${nodeName}?group=${group}`,
  );

  let result = {};
  if (addNodeRes.data.isSuccess) {
    const res = await wait({
      url: `${url}/${name}?group=${group}`,
      checkFn: waitUtil.waitForNodeReady,
      paramRes: nodeName,
    });
    result = responseUtil(res, broker);
  } else {
    result = responseUtil(addNodeRes, broker);
  }
  result.title = result.errors
    ? `Failed to add node ${nodeName} into broker ${name}.`
    : `Successfully added node ${nodeName} into broker ${name}.`;
  return result;
};

export const removeNode = async params => {
  const { name, group, nodeName } = params;
  const removeNodeRes = await axiosInstance.delete(
    `${url}/${name}/${nodeName}?group=${group}`,
  );

  let result = {};
  if (removeNodeRes.data.isSuccess) {
    const res = await wait({
      url: `${url}/${name}?group=${group}`,
      checkFn: waitUtil.waitForNodeNonexistentInCluster,
      paramRes: nodeName,
    });
    result = responseUtil(res, broker);
  } else {
    result = responseUtil(removeNodeRes, broker);
  }
  result.title = result.errors
    ? `Failed to remove node ${nodeName} from broker ${name}.`
    : `Successfully removed node ${nodeName} from broker ${name}.`;
  return result;
};
