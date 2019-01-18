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
  jdbcSource: 'com.island.ohara.connector.jdbc.JDBCSourceConnector',
  ftpSource: 'com.island.ohara.connector.ftp.FtpSource',
  hdfsSink: 'com.island.ohara.connector.hdfs.HDFSSinkConnector',
  ftpSink: 'com.island.ohara.connector.ftp.FtpSink',
  topic: 'topic',
};

export const ICON_MAPS = {
  [CONNECTOR_TYPES.jdbcSource]: 'fa-file-import',
  [CONNECTOR_TYPES.ftpSource]: 'fa-file-import',
  [CONNECTOR_TYPES.topic]: 'fa-list-ul',
  [CONNECTOR_TYPES.hdfsSink]: 'fa-file-export',
  [CONNECTOR_TYPES.ftpSink]: 'fa-file-export',
};

export const TABLE_HEADERS = ['connector name', 'version', 'revision'];

export const CONNECTOR_FILTERS = [
  'com.island.ohara.configurator.endpoint.Validator',
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
