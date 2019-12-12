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

import * as connector from './body/connectorBody';
import {
  getKey,
  requestUtil,
  responseUtil,
  axiosInstance,
} from './utils/apiUtils';
import * as URL from './utils/url';
import wait from './waitApi';
import * as waitUtil from './utils/waitUtils';
import * as inspectApi from './inspectApi';

const url = URL.CONNECTOR_URL;

export const connectorSources = {
  jdbc: 'com.island.ohara.connector.jdbc.source.JDBCSourceConnector',
  json: 'com.island.ohara.connector.jio.JsonIn',
  ftp: 'com.island.ohara.connector.ftp.FtpSource',
  smb: 'com.island.ohara.connector.smb.SmbSource',
  perf: 'com.island.ohara.connector.perf.PerfSource',
};

export const connectorSinks = {
  json: 'com.island.ohara.connector.jio.JsonOut',
  ftp: 'com.island.ohara.connector.ftp.FtpSink',
  hdfs: 'com.island.ohara.connector.hdfs.sink.HDFSSink',
  smb: 'com.island.ohara.connector.smb.SmbSink',
  console: 'com.island.ohara.connector.console.ConsoleSink',
};

export const create = async params => {
  const info = params.classInfos
    ? { data: { classInfos: params.classInfos } }
    : await inspectApi.getWorkerInfo(params.workerClusterKey);

  let connectorDefinition = {};
  if (!info.errors) {
    const connectorDefinitions = info.data.classInfos
      .reduce((acc, cur) => acc.concat(cur), [])
      // the "connector__class" will be convert to "connector.class" for request
      // each connector creation must assign connector.class
      .filter(param => param.className === params.connector__class);
    if (connectorDefinitions.length > 0)
      connectorDefinition = connectorDefinitions[0];
  }
  const requestBody = requestUtil(params, connector, connectorDefinition);
  const res = await axiosInstance.post(url, requestBody);
  const result = responseUtil(res, connector);
  result.title =
    `Create connector ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const start = async params => {
  const { name, group } = params;
  await axiosInstance.put(`${url}/${name}/start?group=${group}`);
  const res = await wait({
    url: `${url}/${name}?group=${group}`,
    checkFn: waitUtil.waitForConnectRunning,
  });
  const result = responseUtil(res, connector);
  result.title =
    `Start connector ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const update = async params => {
  const { name, group } = params;
  delete params[name];
  delete params[group];
  const body = params;
  const res = await axiosInstance.put(`${url}/${name}?group=${group}`, body);
  const result = responseUtil(res, connector);
  result.title =
    `Update connector ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const stop = async params => {
  const { name, group } = params;
  await axiosInstance.put(`${url}/${name}/stop?group=${group}`);
  const res = await wait({
    url: `${url}/${name}?group=${group}`,
    checkFn: waitUtil.waitForConnectStop,
  });
  const result = responseUtil(res, connector);
  result.title =
    `Stop connector ${getKey(params)} ` +
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
  const result = responseUtil(res, connector);
  result.title =
    `Remove connector ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const get = async params => {
  const { name, group } = params;
  const res = await axiosInstance.get(`${url}/${name}?group=${group}`);
  const result = responseUtil(res, connector);
  result.title =
    `Get connector ${getKey(params)} ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const getAll = async (params = {}) => {
  const res = await axiosInstance.get(url + URL.toQueryParameters(params));
  const result = responseUtil(res, connector);
  result.title =
    `Get connector list ` + (result.errors ? 'failed.' : 'successful.');
  return result;
};
