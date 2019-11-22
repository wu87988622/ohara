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

import * as validateBasicBody from './body/validateBasicBody';
import * as validateRdbBody from './body/validateRdbBody';
import { responseUtil, axiosInstance } from './utils/apiUtils';
import * as URL from './utils/url';

const url = URL.VALIDATE_URL;

export const resource = {
  hdfs: 'hdfs',
  rdb: 'rdb',
  ftp: 'ftp',
  node: 'node',
  connector: 'connector',
};

export const validateHdfs = async params => {
  const res = await axiosInstance.put(`${url}/${resource.hdfs}`, params);
  return responseUtil(res, validateBasicBody);
};

export const validateRdb = async params => {
  const res = await axiosInstance.put(`${url}/${resource.rdb}`, params);
  return responseUtil(res, validateRdbBody);
};

export const validateFtp = async params => {
  const res = await axiosInstance.put(`${url}/${resource.ftp}`, params);
  return responseUtil(res, validateBasicBody);
};

export const validateNode = async params => {
  const res = await axiosInstance.put(`${url}/${resource.node}`, params);
  return responseUtil(res, validateBasicBody);
};

export const validateConnector = async params => {
  const res = await axiosInstance.put(`${url}/${resource.connector}`, params);
  return responseUtil(res, {});
};
