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

import {
  BrokerProvider,
  useBrokerState,
  useBrokerDispatch,
  useBrokerActions,
} from 'context/broker/BrokerContext';
import {
  DialogProvider,
  useAddTopicDialog,
  useViewTopicDialog,
  useEditWorkspaceDialog,
  useDevToolDialog,
} from 'context/dialog/DialogContext';
import {
  FileProvider,
  useFileState,
  useFileActions,
} from 'context/file/FileContext';
import {
  TopicProvider,
  useTopicState,
  useTopicActions,
} from 'context/topic/TopicContext';
import {
  WorkerProvider,
  useWorkerState,
  useWorkerDispatch,
  useWorkerActions,
} from 'context/worker/WorkerContext';
import {
  WorkspaceProvider,
  useWorkspace,
} from 'context/workspace/WorkspaceContext';
import {
  ZookeeperProvider,
  useZookeeperState,
  useZookeeperDispatch,
  useZookeeperActions,
} from 'context/zookeeper/ZookeeperContext';

export {
  // Broker
  BrokerProvider,
  useBrokerState,
  useBrokerDispatch,
  useBrokerActions,
  // Dialog
  DialogProvider,
  useViewTopicDialog,
  useEditWorkspaceDialog,
  useAddTopicDialog,
  useDevToolDialog,
  // File
  FileProvider,
  useFileState,
  useFileActions,
  // Topic
  TopicProvider,
  useTopicState,
  useTopicActions,
  // Worker
  WorkerProvider,
  useWorkerState,
  useWorkerDispatch,
  useWorkerActions,
  // Workspace
  WorkspaceProvider,
  useWorkspace,
  // Zookeeper
  ZookeeperProvider,
  useZookeeperState,
  useZookeeperDispatch,
  useZookeeperActions,
};
