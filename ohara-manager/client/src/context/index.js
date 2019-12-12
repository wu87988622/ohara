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
  useGraphSettingDialog,
  useListWorkspacesDialog,
} from 'context/dialog/DialogContext';
import {
  FileProvider,
  useFileState,
  useFileActions,
} from 'context/file/FileContext';
import {
  NodeProvider,
  useNodeState,
  useNodeDispatch,
  useNodeActions,
} from 'context/node/NodeContext';
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
  EditWorkspaceProvider,
  useEditWorkspace,
} from 'context/workspace/EditWorkspaceContext';
import {
  ZookeeperProvider,
  useZookeeperState,
  useZookeeperDispatch,
  useZookeeperActions,
} from 'context/zookeeper/ZookeeperContext';
import {
  PipelineProvider,
  usePipelineState,
  usePipelineDispatch,
  usePipelineActions,
} from 'context/pipeline/PipelineContext';

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
  useGraphSettingDialog,
  useListWorkspacesDialog,
  // File
  FileProvider,
  useFileState,
  useFileActions,
  // Node
  NodeProvider,
  useNodeState,
  useNodeDispatch,
  useNodeActions,
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
  EditWorkspaceProvider,
  useEditWorkspace,
  // Zookeeper
  ZookeeperProvider,
  useZookeeperState,
  useZookeeperDispatch,
  useZookeeperActions,
  // Pipeline
  PipelineProvider,
  usePipelineState,
  usePipelineDispatch,
  usePipelineActions,
};
