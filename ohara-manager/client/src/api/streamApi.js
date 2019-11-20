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

import * as stream from './body/streamBody';
import { requestUtil, responseUtil, axiosInstance } from './utils/apiUtils';
import * as URL from './utils/url';
import wait from './waitApi';
import * as waitUtil from './utils/waitUtils';
import * as file from './fileApi';
import { classType } from './inspectApi';

const url = URL.STREAM_URL;

export const create = async (params, body) => {
  if (!body) {
    // get the stream definition by the required jar file
    const result = await file.get({
      group: params.jarKey.group,
      name: params.jarKey.name,
    });
    const { classInfos } = result;
    // we only support one stream class right now
    // find the first match result
    body = classInfos.find(info => info.classType === classType.stream);
  }
  const requestBody = requestUtil(params, stream, body);
  const res = await axiosInstance.post(url, requestBody);
  return responseUtil(res, stream);
};

export const update = async params => {
  const { name, group } = params;
  delete params[name];
  delete params[group];
  const body = params;
  const res = await axiosInstance.put(`${url}/${name}?group=${group}`, body);
  return responseUtil(res, stream);
};

export const remove = async params => {
  const { name, group } = params;
  await axiosInstance.delete(`${url}/${name}?group=${group}`);
  const res = await wait({
    url,
    checkFn: waitUtil.waitForClusterNonexistent,
    paramRes: params,
  });
  return responseUtil(res, stream);
};

export const start = async params => {
  const { name, group } = params;
  await axiosInstance.put(`${url}/${name}/start?group=${group}`);
  const res = await wait({
    url: `${url}/${name}?group=${group}`,
    checkFn: waitUtil.waitForRunning,
  });
  return responseUtil(res, stream);
};

export const stop = async params => {
  const { name, group } = params;
  await axiosInstance.put(`${url}/${name}/stop?group=${group}`);
  const res = await wait({
    url: `${url}/${name}?group=${group}`,
    checkFn: waitUtil.waitForStop,
  });
  return responseUtil(res, stream);
};

export const get = async params => {
  const { name, group } = params;
  const res = await axiosInstance.get(`${url}/${name}?group=${group}`);
  return responseUtil(res, stream);
};

export const getAll = async (params = {}) => {
  const res = await axiosInstance.get(url + URL.toQueryParameters(params));
  return res ? responseUtil(res, stream) : [];
};
