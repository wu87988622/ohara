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

import { ApiProvider, useApi } from 'context/api/ApiContext';
import { AppProvider, useApp } from 'context/app/AppContext';
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
  useAddNodeDialog,
  useEditNodeDialog,
  useViewNodeDialog,
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
  useWorkspaceState,
  useWorkspaceDispatch,
  useWorkspaceActions,
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
import {
  ConfiguratorProvider,
  useConfiguratorState,
} from 'context/configurator/ConfiguratorContext';
import {
  StreamProvider,
  useStreamState,
  useStreamDispatch,
  useStreamActions,
} from 'context/stream/StreamContext';

import { useSnackbar } from './SnackbarContext';

export {
  // Api
  ApiProvider,
  useApi,
  // App
  AppProvider,
  useApp,
  // Broker
  BrokerProvider,
  useBrokerState,
  useBrokerDispatch,
  useBrokerActions,
  // Dialog
  DialogProvider,
  useViewTopicDialog,
  useAddNodeDialog,
  useEditNodeDialog,
  useViewNodeDialog,
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
  useWorkspaceState,
  useWorkspaceDispatch,
  useWorkspaceActions,
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
  // Configurator
  ConfiguratorProvider,
  useConfiguratorState,
  // Stream
  StreamProvider,
  useStreamState,
  useStreamDispatch,
  useStreamActions,
  // snackBar
  useSnackbar,
};
