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

enum Necessary {
  REQUIRED = 'REQUIRED',
  OPTIONAL = 'OPTIONAL',
  RANDOM_DEFAULT = 'RANDOM_DEFAULT',
}

enum Reference {
  NONE = 'NONE',
  TOPIC = 'TOPIC',
  ZOOKEEPER_CLUSTER = 'ZOOKEEPER_CLUSTER',
  BROKER_CLUSTER = 'BROKER_CLUSTER',
  WORKER_CLUSTER = 'WORKER_CLUSTER',
  FILE = 'FILE',
}

enum Permission {
  READ_ONLY = 'READ_ONLY',
  CREATE_ONLY = 'CREATE_ONLY',
  EDITABLE = 'EDITABLE',
}

enum Type {
  BOOLEAN = 'BOOLEAN',
  STRING = 'STRING',
  POSITIVE_SHORT = 'POSITIVE_SHORT',
  SHORT = 'SHORT',
  POSITIVE_INT = 'POSITIVE_INT',
  INT = 'INT',
  POSITIVE_LONG = 'POSITIVE_LONG',
  LONG = 'LONG',
  POSITIVE_DOUBLE = 'POSITIVE_DOUBLE',
  DOUBLE = 'DOUBLE',
  ARRAY = 'ARRAY',
  CLASS = 'CLASS',
  PASSWORD = 'PASSWORD',
  JDBC_TABLE = 'JDBC_TABLE',
  TABLE = 'TABLE',
  DURATION = 'DURATION',
  REMOTE_PORT = 'REMOTE_PORT',
  BINDING_PORT = 'BINDING_PORT',
  OBJECT_KEY = 'OBJECT_KEY',
  OBJECT_KEYS = 'OBJECT_KEYS',
  TAGS = 'TAGS',
}

interface TableColumn {
  name: string;
  type: string;
  recommendedValues: string[];
}

export interface SettingDef {
  key: string;
  group: string;
  displayName: string;
  orderInGroup: number;
  valueType: Type;
  necessary: Necessary;
  defaultValue: any;
  documentation: string;
  reference: Reference | null;
  regex: string | null;
  internal: boolean;
  permission: Permission;
  tableKeys: TableColumn[];
  recommendedValues: string[];
  blacklist: string[];
  prefix: string | null;
}

export interface ClassInfo {
  classType: string;
  className: string;
  settingDefinitions: SettingDef[];
}
