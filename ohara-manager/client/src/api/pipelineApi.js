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

import { get as lodashGet } from 'lodash';

import * as pipeline from './body/pipelineBody';
import { requestUtil, responseUtil, axiosInstance } from './utils/apiUtils';
import * as URL from './utils/url';
import wait from './waitApi';
import * as waitUtil from './utils/waitUtils';

const url = URL.PIPELINE_URL;

export const create = async (params = {}) => {
  const requestBody = requestUtil(params, pipeline);
  const res = await axiosInstance.post(url, requestBody);
  return responseUtil(res, pipeline);
};

export const update = async params => {
  const { name, group } = params;
  delete params[name];
  delete params[group];
  const body = params;
  const res = await axiosInstance.put(`${url}/${name}?group=${group}`, body);
  return responseUtil(res, pipeline);
};

export const remove = async (params = {}) => {
  const { name, group } = params.settings;
  await axiosInstance.delete(`${url}/${name}?group=${group}`);
  const res = await wait({
    url,
    checkFn: waitUtil.waitForClusterNonexistent,
    paramRes: params,
  });
  return responseUtil(res, pipeline);
};

export const get = async (params = {}) => {
  const { name, group } = params.settings;
  const res = await axiosInstance.get(`${url}/${name}?group=${group}`);
  return responseUtil(res, pipeline);
};

export const getAll = async (params = {}) => {
  const res = await axiosInstance.get(url + URL.toQueryParameters(params));
  return lodashGet(res, 'data.result', []);
};

export const refresh = async (params = {}) => {
  const { name, group } = params;
  await axiosInstance.put(`${url}/${name}/refresh?group=${group}`);
  return get(params);
};
