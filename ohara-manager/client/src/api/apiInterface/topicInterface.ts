/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License',;
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

interface PartitionNode {
  id: number;
  host: string;
  port: number;
}
interface PartitionInfo {
  id: number;
  leader: PartitionNode;
  replicas: PartitionNode[];
  inSyncReplicas: PartitionNode[];
  beginningOffset: number;
  endOffset: number;
}
interface TopicData {
  state?: 'RUNNING';
  partitionInfos: PartitionInfo[];
  metrics: Metrics;
  lastModified: number;
  [k: string]: any;
}
export interface TopicResponse extends BasicResponse {
  data: TopicData;
}
export interface TopicResponseList extends BasicResponse {
  data: TopicData[];
}
