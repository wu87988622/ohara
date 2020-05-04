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

import { BasicResponse, ObjectKey, Metrics } from './basicInterface';

interface EndPoint {
  name: string;
  group: string;
  kind: string;
}
interface ObjectAbstract {
  group: string;
  name: string;
  kind: string;
  className?: string;
  state?: string;
  error?: string;
  nodeMetrics: {
    [hostname: string]: Metrics;
  };
  lastModified: number;
  tags: {
    [k: string]: any;
  };
}
interface Pipeline {
  name: string;
  group: string;
  endpoints: EndPoint[];
  objects: ObjectAbstract[];
  jarKeys: ObjectKey[];
  lastModified: number;
  tags: {
    [k: string]: any;
  };
}

export interface PipelineRequest {
  name: string;
  group: string;
  endpoints: EndPoint[];
  objects: ObjectAbstract[];
  jarKeys: ObjectKey[];
  lastModified: number;
  tags: {
    [k: string]: any;
  };
}

export interface PipelineResponse extends BasicResponse {
  data: Pipeline;
}
export interface PipelineResponseList extends BasicResponse {
  data: Pipeline[];
}
