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

import { BasicResponse } from './basicInterface';

export enum SERVICE_STATE {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  FAILED = 'FAILED',
  UNKNOWN = 'UNKNOWN',
}

export interface VolumeData {
  lastModified: number;
  nodeNames: string[];
  path: string;
  state?: SERVICE_STATE;
  [k: string]: any;
}

export interface VolumeResponse extends BasicResponse {
  data: VolumeData;
}

export interface VolumeResponseList extends BasicResponse {
  data: VolumeData[];
}
