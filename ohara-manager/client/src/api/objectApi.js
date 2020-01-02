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

import * as objBody from './body/objectBody';
import { getKey, responseUtil, axiosInstance } from './utils/apiUtils';
import * as URL from './utils/url';
import wait from './waitApi';
import * as waitUtil from './utils/waitUtils';

const url = URL.OBJECT_URL;

export const create = async (params = {}) => {
  const res = await axiosInstance.post(url, params);

  const result = responseUtil(res, objBody);
  result.title =
    `Create object ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const update = async params => {
  const { name, group } = params;
  delete params[name];
  delete params[group];
  const body = params;
  const res = await axiosInstance.put(`${url}/${name}?group=${group}`, body);
  const result = responseUtil(res, objBody);
  result.title =
    `Update object ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const remove = async params => {
  const { name, group } = params;
  const deleteRes = await axiosInstance.delete(`${url}/${name}?group=${group}`);

  let result = {};
  if (deleteRes.data.isSuccess) {
    const res = await wait({
      url,
      checkFn: waitUtil.waitForObjectNonexistent,
      paramRes: params,
    });
    result = responseUtil(res, objBody);
  } else {
    result = responseUtil(deleteRes, objBody);
  }

  result.title =
    `Remove object ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const get = async params => {
  const { name, group } = params;
  const res = await axiosInstance.get(`${url}/${name}?group=${group}`);
  const result = responseUtil(res, objBody);
  result.title =
    `Get object ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const getAll = async (params = {}) => {
  const res = await axiosInstance.get(url + URL.toQueryParameters(params));
  const result = responseUtil(res, objBody);
  result.title =
    `Get object list ` + (result.errors ? 'failed.' : 'successful.');
  return result;
};
