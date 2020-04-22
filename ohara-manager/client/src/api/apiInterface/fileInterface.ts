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
import { ClassInfo } from './definitionInterface';

export interface FileRequest {
  name: string;
  group: string;
  file: Blob;
  tags?: object;
}

interface Data {
  name: string;
  group: string;
  url?: string;
  size: number;
  classInfos: ClassInfo[];
  lastModified: number;
  tags: {
    [k: string]: any;
  };
}
export interface FileResponse extends BasicResponse {
  data: Data;
}
export interface FileResponseList extends BasicResponse {
  data: Data[];
}
