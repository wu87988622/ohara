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

export const CONNECTOR_TYPES = {
  jdbcSource: 'com.island.ohara.connector.jdbc.source.JDBCSourceConnector',
  ftpSource: 'com.island.ohara.connector.ftp.FtpSource',
  hdfsSink: 'com.island.ohara.connector.hdfs.sink.HDFSSink',
  ftpSink: 'com.island.ohara.connector.ftp.FtpSink',
  customSource: 'com.island.ohara.it.connector.DumbSourceConnector',
  customSink: 'com.island.ohara.it.connector.DumbSinkConnector',
  streamApp: 'streamApp',
  topic: 'topic',
};

export const TABLE_HEADERS = ['connector name', 'version', 'revision'];

export const CONNECTOR_FILTERS = [
  'com.island.ohara.connector.validation.Validator',
  'com.island.ohara.connector.perf.PerfSource',
  'org.apache.kafka.connect.file.FileStreamSourceConnector',
  'org.apache.kafka.connect.file.FileStreamSinkConnector',
];

export const CONNECTOR_STATES = {
  unassigned: 'UNASSIGNED',
  running: 'RUNNING',
  paused: 'PAUSED',
  failed: 'FAILED',
  destroyed: 'DESTROYED',
};

export const CONNECTOR_ACTIONS = {
  start: 'start',
  pause: 'pause',
  resume: 'resume',
  stop: 'stop',
};

export const STREAM_APP_STATES = {
  running: 'RUNNING',
  failed: 'FAILED',
};

export const STREAM_APP_ACTIONS = {
  start: 'start',
  stop: 'stop',
};
