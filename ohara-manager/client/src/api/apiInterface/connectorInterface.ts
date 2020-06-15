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

import { BasicResponse, Metrics } from './basicInterface';

export enum SOURCES {
  jdbc = 'oharastream.ohara.connector.jdbc.source.JDBCSourceConnector',
  ftp = 'oharastream.ohara.connector.ftp.FtpSource',
  smb = 'oharastream.ohara.connector.smb.SmbSource',
  perf = 'oharastream.ohara.connector.perf.PerfSource',
  shabondi = 'oharastream.ohara.shabondi.ShabondiSource',
}

export enum SINKS {
  console = 'oharastream.ohara.connector.console.ConsoleSink',
  ftp = 'oharastream.ohara.connector.ftp.FtpSink',
  hdfs = 'oharastream.ohara.connector.hdfs.sink.HDFSSink',
  smb = 'oharastream.ohara.connector.smb.SmbSink',
  shabondi = 'oharastream.ohara.shabondi.ShabondiSink',
}

export enum State {
  UNASSIGNED = 'UNASSIGNED',
  RUNNING = 'RUNNING',
  PAUSED = 'PAUSED',
  FAILED = 'FAILED',
  DESTROYED = 'DESTROYED',
}
interface Status {
  state: State;
  nodeName: string;
  error?: string;
  coordinator: boolean;
}
interface Data {
  state?: State;
  aliveNodes: string[];
  error?: string;
  tasksStatus: Status[];
  nodeMetrics: {
    [nodeName: string]: Metrics;
  };
  lastModified: number;
  [k: string]: any;
}
export interface ConnectorResponse extends BasicResponse {
  data: Data;
}
export interface ConnectorResponseList extends BasicResponse {
  data: Data[];
}
