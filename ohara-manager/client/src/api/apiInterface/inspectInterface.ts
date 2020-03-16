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

import { MODE } from 'const';
import { BasicResponse, ObjectKey } from './basicInterface';
import { SettingDef, ClassInfo } from './definitionInterface';

interface InspectServiceBody {
  imageName?: string;
  settingDefinitions: SettingDef[];
  classInfos: ClassInfo[];
}

// Request
export interface InspectTopicRequest {
  key: ObjectKey;
  limit: number;
  timeout: number;
}

export interface InspectRdbRequest {
  url: string;
  user: string;
  password: string;
  workerClusterKey: ObjectKey;
  catalogPattern?: string;
  schemaPattern?: string;
  tableName: string;
}

// Response
export interface InspectServiceResponse extends BasicResponse {
  data: InspectServiceBody;
}

export interface InspectConfiguratorResponse extends BasicResponse {
  data: {
    versionInfo: {
      version: string;
      branch: string;
      user: string;
      revision: string;
      date: string;
    };
    mode: MODE;
  };
}

export interface InspectManagerResponse extends BasicResponse {
  data: {
    version: string;
    branch: string;
    user: string;
    revision: string;
    date: string;
  };
}

interface Message {
  partition: number;
  offset: number;
  sourceClass?: string;
  sourceKey?: string;
  value?: object;
  error?: string;
}
export interface InspectTopicResponse extends BasicResponse {
  data: {
    messages: Message[];
  };
}

interface RdbColumn {
  name: string;
  dataType: string;
  pk: boolean;
}
interface RdbTable {
  catalogPattern?: string;
  schemaPattern?: string;
  name: string;
  columns: RdbColumn[];
}
export interface InspectRdbResponse extends BasicResponse {
  data: {
    name: string;
    tables: RdbTable[];
  };
}
