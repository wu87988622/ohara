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

import { of, Observable } from 'rxjs';
import { delay } from 'rxjs/operators';
import { InspectServiceResponse } from 'api/apiInterface/inspectInterface';
import {
  Type,
  Necessary,
  Reference,
  Permission,
} from 'api/apiInterface/definitionInterface';

export const brokerInfoEntity = {
  imageName: 'oharastream/broker:0.10.0-SNAPSHOT',
  settingDefinitions: [
    {
      blacklist: [],
      reference: Reference.NONE,
      displayName: 'xmx',
      regex: null,
      internal: false,
      permission: Permission.EDITABLE,
      documentation: 'maximum memory allocation (in MB)',
      necessary: Necessary.OPTIONAL,
      valueType: Type.POSITIVE_LONG,
      tableKeys: [],
      orderInGroup: 8,
      key: 'xmx',
      defaultValue: 1024,
      recommendedValues: [],
      group: 'core',
      prefix: '',
    },
  ],
  classInfos: [],
};

export const zookeeperInfoEntity = {
  imageName: 'oharastream/zookeeper:0.10.0-SNAPSHOT',
  settingDefinitions: [
    {
      blacklist: [],
      reference: Reference.NONE,
      displayName: 'peerPort',
      regex: null,
      internal: false,
      permission: Permission.EDITABLE,
      documentation: 'the port exposed to each quorum',
      necessary: Necessary.RANDOM_DEFAULT,
      valueType: Type.BINDING_PORT,
      tableKeys: [],
      orderInGroup: 10,
      key: 'peerPort',
      defaultValue: null,
      recommendedValues: [],
      group: 'core',
      prefix: '',
    },
  ],
  classInfos: [],
};

// simulate a promise request with a delay of 3s
export const getBrokerInfo = (): Observable<InspectServiceResponse> =>
  of({
    status: 200,
    title: 'mock inspect broker data',
    data: brokerInfoEntity,
  }).pipe(delay(3000));

// simulate a promise request with a delay of 3s
export const getZookeeperInfo = (): Observable<InspectServiceResponse> =>
  of({
    status: 200,
    title: 'mock inspect zookeeper data',
    data: zookeeperInfoEntity,
  }).pipe(delay(3000));
