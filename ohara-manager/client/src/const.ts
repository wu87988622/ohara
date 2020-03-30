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

// kind of all objects
export enum KIND {
  configurator = 'configurator',
  zookeeper = 'zookeeper',
  broker = 'broker',
  worker = 'worker',
  stream = 'stream',
  sink = 'sink',
  source = 'source',
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

export enum CREATE_WORKSPACE_MODE {
  QUICK = 'quick',
  EXPERT = 'expert',
}

export enum GROUP {
  WORKSPACE = 'workspace',
  ZOOKEEPER = 'zookeeper',
  BROKER = 'broker',
  WORKER = 'worker',
}

export enum FORM {
  CREATE_WORKSPACE = 'createWorkspace',
}
