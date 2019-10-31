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

import * as node from './body/nodeBody';
import { requestUtil, responseUtil, axiosInstance } from './utils/apiUtils';
import * as URL from './utils/url';
import wait from './waitApi';
import * as waitUtil from './utils/waitUtils';

const url = URL.NODE_URL;

export const create = async (params = {}) => {
  const requestBody = requestUtil(params, node);
  const res = await axiosInstance.post(url, requestBody);
  return responseUtil(res, node);
};

export const update = async params => {
  const { hostname } = params;
  delete params[hostname];
  const body = params;
  const res = await axiosInstance.put(`${url}/${hostname}`, body);
  return responseUtil(res, node);
};

export const remove = async (params = {}) => {
  const { hostname } = params;
  await axiosInstance.delete(`${url}/${hostname}`);
  const res = await wait({
    url,
    checkFn: waitUtil.waitForNodeNonexistent,
    paramRes: params,
  });
  return responseUtil(res, node);
};

export const get = async (params = {}) => {
  const { hostname } = params;
  const res = await axiosInstance.get(`${url}/${hostname}`);
  return responseUtil(res, node);
};

export const getAll = async (params = {}) => {
  const res = await axiosInstance.get(url + URL.toQueryParameters(params));
  return res ? responseUtil(res, node) : [];
};
