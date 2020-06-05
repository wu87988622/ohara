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

export enum Necessary {
  REQUIRED = 'REQUIRED',
  OPTIONAL = 'OPTIONAL',
  RANDOM_DEFAULT = 'RANDOM_DEFAULT',
}

export enum Reference {
  NONE = 'NONE',
  TOPIC = 'TOPIC',
  ZOOKEEPER = 'ZOOKEEPER',
  BROKER = 'BROKER',
  WORKER = 'WORKER',
  FILE = 'FILE',
  NODE = 'NODE',
}

export enum Permission {
  READ_ONLY = 'READ_ONLY',
  CREATE_ONLY = 'CREATE_ONLY',
  EDITABLE = 'EDITABLE',
}

export enum Type {
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

export function isNumberType(type: Type) {
  switch (type) {
    case Type.POSITIVE_SHORT:
    case Type.SHORT:
    case Type.POSITIVE_INT:
    case Type.INT:
    case Type.POSITIVE_LONG:
    case Type.LONG:
    case Type.POSITIVE_DOUBLE:
    case Type.DOUBLE:
    case Type.REMOTE_PORT:
    case Type.BINDING_PORT:
      return true;
    default:
      return false;
  }
}

export function isStringType(type: Type) {
  switch (type) {
    case Type.STRING:
    case Type.CLASS:
    case Type.PASSWORD:
    case Type.JDBC_TABLE:
    case Type.DURATION:
      return true;
    default:
      return false;
  }
}

export function isArrayType(type: Type) {
  switch (type) {
    case Type.ARRAY:
    case Type.OBJECT_KEYS:
    case Type.TABLE:
      return true;
    default:
      return false;
  }
}

export function isObjectType(type: Type) {
  switch (type) {
    case Type.OBJECT_KEY:
    case Type.TAGS:
      return true;
    default:
      return false;
  }
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
  defaultValue?: any;
  documentation: string;
  reference?: Reference;
  regex?: string;
  internal: boolean;
  permission: Permission;
  tableKeys: TableColumn[];
  recommendedValues: string[];
  blacklist: string[];
  prefix?: string;
}

export class UISettingDef implements SettingDef {
  key: string;
  group: string;
  displayName: string;
  orderInGroup: number;
  valueType: Type;
  necessary: Necessary;
  defaultValue?: any;
  documentation: string;
  reference?: Reference | undefined;
  regex?: string | undefined;
  internal: boolean;
  permission: Permission;
  tableKeys: TableColumn[];
  recommendedValues: string[];
  blacklist: string[];
  prefix?: string | undefined;

  constructor(key: string, type: Type, internal: boolean = true) {
    this.key = key;
    this.group = 'default';
    this.displayName = key;
    this.orderInGroup = -1;
    this.valueType = type;
    this.necessary = Necessary.REQUIRED;
    this.documentation = `${key} of UI SettingDef`;
    this.internal = internal;
    this.permission = Permission.CREATE_ONLY;
    this.tableKeys = [];
    this.recommendedValues = [];
    this.blacklist = [];
  }
}

export interface ClassInfo {
  classType: string;
  className: string;
  settingDefinitions: SettingDef[];
}
