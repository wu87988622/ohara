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

import { BasicResponse, ObjectKey } from './basicInterface';

export enum LOG_SERVICES {
  configurator = 'configurator',
  zookeeper = 'zookeepers',
  broker = 'brokers',
  worker = 'workers',
  shabondi = 'shabondis',
  stream = 'streams',
}

interface NodeLog {
  hostname: string;
  value: string;
}
export interface LogResponse extends BasicResponse {
  data: {
    clusterKey: ObjectKey;
    logs: NodeLog[];
  };
}
