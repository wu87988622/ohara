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

import { omit } from 'lodash';
import { RESOURCE, API, COMMAND } from '../api/utils/apiUtils';
import { ObjectKey, BasicResponse } from './apiInterface/basicInterface';
import {
  VolumeResponse,
  VolumeResponseList,
} from './apiInterface/volumeInterface';
import { ServiceBody } from './apiInterface/clusterInterface';

const volumeApi = new API(RESOURCE.VOLUME);

export const create = (params: ServiceBody) => {
  return volumeApi.post<VolumeResponse>({
    name: params.name,
    body: params,
  });
};

export const update = (params: ServiceBody) => {
  return volumeApi.put<VolumeResponse>({
    name: params.name,
    group: params.group,
    body: omit(params, ['name', 'group']),
  });
};

export const remove = (objectKey: ObjectKey) => {
  return volumeApi.delete<BasicResponse>(objectKey);
};

export const get = (objectKey: ObjectKey) => {
  return volumeApi.get<VolumeResponse>({
    name: objectKey.name,
    queryParams: { group: objectKey.group },
  });
};

export const getAll = (queryParams?: object) => {
  return volumeApi.get<VolumeResponseList>({ queryParams });
};

export const start = (objectKey: ObjectKey) => {
  return volumeApi.execute<BasicResponse>({
    objectKey,
    action: COMMAND.START,
  });
};

export const stop = (objectKey: ObjectKey) => {
  return volumeApi.execute<BasicResponse>({
    objectKey,
    action: COMMAND.STOP,
  });
};
