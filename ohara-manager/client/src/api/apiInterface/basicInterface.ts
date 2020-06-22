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

export interface Params {
  [key: string]: string;
}

export interface Meta {
  url?: string;
  method?: string;
  params?: Params;
  body?: any;
}

export interface ApiError {
  code: string;
  message: string;
  stack: string;
  apiUrl?: string;
}

export interface BasicResponse {
  status: number;
  // object or array
  data: object | object[];
  // api message that could be used in SnackBar
  title: string;
  // additional information about this response
  meta?: Meta;
}

export interface ObjectKey {
  name: string;
  group: string;
}

export interface ObjectData extends ObjectKey {
  [k: string]: any;
}

interface Meter {
  name: string;
  value: number;
  valueInPerSec?: number;
  unit: string;
  document: string;
  queryTime: number;
  startTime?: number;
  lastModified?: number;
}
export interface Metrics {
  meters: Meter[];
}
