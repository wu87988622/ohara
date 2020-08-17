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

import { schema } from 'normalizr';
import { getId } from 'utils/object';

export const ENTITY_TYPE = {
  brokers: 'brokers',
  connectors: 'connectors',
  eventLogs: 'eventLogs',
  files: 'files',
  infos: 'infos',
  nodes: 'nodes',
  pipelines: 'pipelines',
  shabondis: 'shabondis',
  streams: 'streams',
  topics: 'topics',
  volumes: 'volumes',
  workers: 'workers',
  workspaces: 'workspaces',
  zookeepers: 'zookeepers',
};

export const broker = new schema.Entity(ENTITY_TYPE.brokers, undefined, {
  idAttribute: getId,
});

export const connector = new schema.Entity(ENTITY_TYPE.connectors, undefined, {
  idAttribute: getId,
});

export const eventLog = new schema.Entity(ENTITY_TYPE.eventLogs, undefined);

export const file = new schema.Entity(ENTITY_TYPE.files, undefined, {
  idAttribute: getId,
});

export const info = new schema.Entity(ENTITY_TYPE.infos, undefined, {
  idAttribute: getId,
});

export const node = new schema.Entity(ENTITY_TYPE.nodes, undefined, {
  idAttribute: (node) => node.hostname,
});

export const pipeline = new schema.Entity(ENTITY_TYPE.pipelines, undefined, {
  idAttribute: getId,
});

export const shabondi = new schema.Entity(ENTITY_TYPE.shabondis, undefined, {
  idAttribute: getId,
});

export const stream = new schema.Entity(ENTITY_TYPE.streams, undefined, {
  idAttribute: getId,
});

export const topic = new schema.Entity(ENTITY_TYPE.topics, undefined, {
  idAttribute: getId,
});

export const volume = new schema.Entity(ENTITY_TYPE.volumes, undefined, {
  idAttribute: getId,
});

export const worker = new schema.Entity(ENTITY_TYPE.workers, undefined, {
  idAttribute: getId,
});

export const workspace = new schema.Entity(ENTITY_TYPE.workspaces, undefined, {
  idAttribute: getId,
});

export const zookeeper = new schema.Entity(ENTITY_TYPE.zookeepers, undefined, {
  idAttribute: getId,
});
