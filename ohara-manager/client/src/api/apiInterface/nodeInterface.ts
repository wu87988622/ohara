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

export enum NODE_STATE {
  AVAILABLE = 'AVAILABLE',
  UNAVAILABLE = 'UNAVAILABLE',
}

interface NodeService {
  name: string;
  clusterKeys: ObjectKey[];
}
interface Resource {
  name: string;
  value: number;
  unit: string;
  used?: number;
}

export interface NodeRequest {
  hostname: string;
  port?: number;
  user?: string;
  password?: string;
  tags?: {
    [k: string]: any;
  };
}
export interface NodeData {
  hostname: string;
  port?: number;
  user?: string;
  password?: string;
  services: NodeService[];
  state: NODE_STATE;
  error?: string;
  lastModified: number;
  resources: Resource[];
  tags: {
    [k: string]: any;
  };
}
export interface NodeResponse extends BasicResponse {
  data: NodeData;
}
export interface NodeResponseList extends BasicResponse {
  data: NodeData[];
}
