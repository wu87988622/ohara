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

export const PACKAGE_ROOT = 'oharastream.ohara';

// kind of all objects
export enum KIND {
  configurator = 'configurator',
  zookeeper = 'zookeeper',
  broker = 'broker',
  worker = 'worker',
  stream = 'stream',
  sink = 'sink',
  source = 'source',
  shabondi = 'shabondi',
  topic = 'topic',
  object = 'object',
}

export enum MODE {
  FAKE = 'FAKE',
  DOCKER = 'DOCKER',
  K8S = 'K8S',
}

export enum CELL_STATUS {
  stopped = 'stopped',
  pending = 'pending',
  running = 'running',
  failed = 'failed',
}

export enum CELL_PROPS {
  cellType = 'type', // JointJS element type
  id = 'id',
  name = 'name',
  kind = 'kind',
  displayName = 'displayName',
  isTemporary = 'isTemporary',
  className = 'className',
  position = 'position',
  jarKey = 'jarKey',
  isShared = 'isShared',
  isSelected = 'isSelected',
  status = 'status',
  source = 'source',
  target = 'target',
}

export enum CELL_TYPES {
  LINK = 'standard.Link',
  ELEMENT = 'html.Element',
}

export enum CREATE_WORKSPACE_MODE {
  QUICK = 'quick',
  EXPERT = 'expert',
}

export enum GROUP {
  DEFAULT = 'default',
  WORKSPACE = 'workspace',
  ZOOKEEPER = 'zookeeper',
  BROKER = 'broker',
  WORKER = 'worker',
}

export enum FORM {
  CREATE_WORKSPACE = 'createWorkspace',
}

export enum LOG_LEVEL {
  info = 'info',
  warning = 'warning',
  error = 'error',
}

export enum LOG_TIME_GROUP {
  latest = 'latest',
  customize = 'customize',
}

export enum SERVICE_NAME {
  BROKER = 'broker',
  WORKER = 'connect-worker',
  STREAM = 'stream',
  ZOOKEEPER = 'zookeeper',
  CONFIGURATOR = 'configurator',
}

export enum CLUSTER_STATE {
  RUNNING = 'RUNNING',
}
export enum SETTINGS_COMPONENT_TYPES {
  DIALOG = 'DIALOG',
  PAGE = 'PAGE',
  CUSTOMIZED = 'CUSTOMIZED',
}

export enum ACTIONS {
  STOP_WORKER = 'STOP_WORKER',
  STOP_BROKER = 'STOP_BROKER',
  STOP_ZOOKEEPER = 'STOP_ZOOKEEPER',
  DELETE_WORKER = 'DELETE_WORKER',
  DELETE_BROKER = 'DELETE_BROKER',
  DELETE_ZOOKEEPER = 'DELETE_ZOOKEEPER',
  CREATE_WORKER = 'CREATE_WORKER',
  CREATE_BROKER = 'CREATE_BROKER',
  CREATE_ZOOKEEPER = 'CREATE_BROKER',
  START_WORKER = 'START_WORKER',
  START_BROKER = 'START_BROKER',
  START_ZOOKEEPER = 'START_ZOOKEEPER',
}
