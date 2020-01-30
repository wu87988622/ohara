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

import { KIND } from '../const';
import * as stream from './body/streamBody';
import { requestUtil, responseUtil, axiosInstance } from './utils/apiUtils';
import * as URL from './utils/url';
import wait from './waitApi';
import * as waitUtil from './utils/waitUtils';
import * as file from './fileApi';

const url = URL.STREAM_URL;

export const create = async (params, body = {}) => {
  if (isEmpty(body)) {
    // get the stream definition by the required jar file
    const result = await file.get({
      group: params.jarKey.group,
      name: params.jarKey.name,
    });
    if (!result.errors) {
      const { classInfos } = result.data;
      // we only support one stream class right now
      // find the first match result
      const classes = classInfos.filter(info => info.classType === KIND.stream);
      if (classes.length > 0) body = classes[0];
    }
  }
  const { name } = params;
  const requestBody = requestUtil(params, stream, body);
  const res = await axiosInstance.post(url, requestBody);
  const result = responseUtil(res, stream);
  result.title = result.errors
    ? `Failed to create stream ${name}.`
    : `Successfully created stream ${name}.`;
  return result;
};

export const update = async params => {
  const { name, group } = params;
  delete params[name];
  delete params[group];
  const body = params;
  const res = await axiosInstance.put(`${url}/${name}?group=${group}`, body);
  const result = responseUtil(res, stream);
  result.title = result.errors
    ? `Failed to update stream ${name}.`
    : `Successfully updated stream ${name}.`;
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
    result = responseUtil(res, stream);
  } else {
    result = responseUtil(deletedRes, stream);
  }
  result.title = result.errors
    ? `Failed to remove stream ${name}.`
    : `Successfully removed stream ${name}.`;
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
    result = responseUtil(res, stream);
  } else {
    result = responseUtil(startRes, stream);
  }
  result.title = result.errors
    ? `Failed to start stream ${name}.`
    : `Successfully started stream ${name}.`;
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
    result = responseUtil(res, stream);
  } else {
    result = responseUtil(stopRes, stream);
  }
  result.title = result.errors
    ? `Failed to stop stream ${name}.`
    : `Successfully stopped stream ${name}.`;
  return result;
};

export const get = async params => {
  const { name, group } = params;
  const res = await axiosInstance.get(`${url}/${name}?group=${group}`);
  const result = responseUtil(res, stream);
  result.title = result.errors
    ? `Failed to get stream ${name}.`
    : `Successfully got stream ${name}.`;
  return result;
};

export const getAll = async (params = {}) => {
  const res = await axiosInstance.get(url + URL.toQueryParameters(params));
  const result = responseUtil(res, stream);
  result.title = result.errors
    ? `Failed to get stream list.`
    : `Successfully got stream list.`;
  return result;
};
