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
import * as inspectConfiguratorBody from './body/inspectConfiguratorBody';
import * as inspectManagerBody from './body/inspectManagerBody';
import * as inspectServiceBody from './body/inspectServiceBody';
import * as inspectTopicBody from './body/inspectTopicBody';
import * as inspectRdbBody from './body/inspectRdbBody';
import * as file from './body/fileBody';
import {
  getKey,
  requestUtil,
  responseUtil,
  axiosInstance,
} from './utils/apiUtils';
import * as URL from './utils/url';

const url = URL.INSPECT_URL;
export const inspectKind = {
  configurator: 'configurator',
  zookeeper: 'zookeeper',
  broker: 'broker',
  worker: 'worker',
  stream: 'stream',
  manager: 'manager',
  rdb: 'rdb',
  topic: 'topic',
  file: 'files',
};

const converterDotToLodash = params => {
  const { settingDefinitions = [], classInfos = [] } = params.data;
  const converter = def => {
    const obj = def;
    Object.keys(def)
      .filter(key => key === 'key' || key === 'group')
      .forEach(key => {
        switch (obj[key]) {
          case 'group':
            obj.internal = true;
            break;

          default:
            if (obj.key.indexOf('.') !== -1) {
              obj.key = obj.key.replace(/\./g, '__');
            }
            break;
        }
      });
    return obj;
  };

  const lodashSettingDefinitions = settingDefinitions.map(def =>
    converter(def),
  );
  const lodashClassInfos = classInfos.map(classInfo => {
    const objs = classInfo;
    objs.settingDefinitions = classInfo.settingDefinitions.map(def =>
      converter(def),
    );
    return objs;
  });
  if (lodashSettingDefinitions.length > 0) {
    params.data.settingDefinitions = lodashSettingDefinitions;
  }
  if (lodashClassInfos.length > 0) {
    params.data.classInfos = lodashClassInfos;
  }

  return params;
};

const fetchServiceInfo = async (kind, params) => {
  const reqUrl = !isEmpty(params)
    ? `${url}/${kind}/${params.name}?group=${params.group}`
    : `${url}/${kind}`;
  const res = await axiosInstance.get(reqUrl);
  const result = responseUtil(res, inspectServiceBody);
  result.title =
    `Inspect ${kind} ${getKey(params)} info ` +
    (result.errors ? 'failed.' : 'successful.');

  const converterResult = converterDotToLodash(result);

  return converterResult;
};

export const getConfiguratorInfo = async () => {
  const res = await axiosInstance.get(`${url}/${inspectKind.configurator}`);
  const result = responseUtil(res, inspectConfiguratorBody);
  result.title =
    `Inspect ${inspectKind.configurator} info ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const getZookeeperInfo = async (params = {}) => {
  return fetchServiceInfo(inspectKind.zookeeper, params);
};

export const getBrokerInfo = async (params = {}) => {
  return fetchServiceInfo(inspectKind.broker, params);
};

export const getWorkerInfo = async (params = {}) => {
  return fetchServiceInfo(inspectKind.worker, params);
};

export const getStreamsInfo = async (params = {}) => {
  return fetchServiceInfo(inspectKind.stream, params);
};

export const getManagerInfo = async () => {
  const res = await axiosInstance.get(`${url}/${inspectKind.manager}`);
  const result = responseUtil(res, inspectManagerBody);
  result.title =
    `Inspect ${inspectKind.manager} info ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const getFileInfoWithoutUpload = async params => {
  const fileUrl = `${url}/${inspectKind.file}`;

  const requestBody = requestUtil(params, file);
  const config = {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  };

  let formData = new FormData();
  formData.append('file', requestBody.file);
  formData.append('group', requestBody.group);
  if (requestBody.tags) {
    formData.append('tags', JSON.stringify(requestBody.tags));
  }
  const res = await axiosInstance.post(fileUrl, formData, config);
  const result = responseUtil(res, file);
  result.title =
    `Inspect ${inspectKind.file} ${getKey(params)} info ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const getTopicData = async params => {
  const { name, ...others } = params;
  const topicUrl =
    `${url}/${inspectKind.topic}/${name}` + URL.toQueryParameters(others);
  const res = await axiosInstance.post(topicUrl);
  const result = responseUtil(res, inspectTopicBody);
  result.title =
    `Inspect ${inspectKind.topic} ${getKey(params)} info ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};

export const getRdbData = async params => {
  const rdbUrl = `${url}/${inspectKind.rdb}`;
  const requestBody = requestUtil(params, inspectRdbBody);
  const res = await axiosInstance.post(rdbUrl, requestBody);
  const result = responseUtil(res, inspectRdbBody);
  result.title =
    `Inspect ${inspectKind.rdb} info ` +
    (result.errors ? 'failed.' : 'successful.');
  return result;
};
