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
export const KIND = {
  configurator: 'configurator',
  zookeeper: 'zookeeper',
  broker: 'broker',
  worker: 'worker',
  stream: 'stream',
  sink: 'sink',
  source: 'source',
  topic: 'topic',
  object: 'object',
};

export const MODE = {
  fake: 'FAKE',
  docker: 'DOCKER',
  k8s: 'K8S',
};

export const CELL_STATUS = {
  stopped: 'stopped',
  pending: 'pending',
  running: 'running',
  failed: 'failed',
};

export const CONNECTION_TYPE = {
  source_topic_sink: 'source_topic_sink',
  source_topic_stream: 'source_topic_stream',
  stream_topic_sink: 'stream_topic_sink',
  source_topic: 'source_topic',
  stream_topic: 'stream_topic',
  topic_sink: 'topic_sink',
  topic_stream: 'topic_stream',
};
