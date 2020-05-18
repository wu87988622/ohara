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

import { useCallback, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { map, size, xor } from 'lodash';

import { GROUP } from 'const';
import * as hooks from 'hooks';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';

export const useWorkspaceGroup = () => GROUP.WORKSPACE;

export const useIsWorkspaceReady = () => {
  const mapState = useCallback(state => !!state.ui.workspace.lastUpdated, []);
  return useSelector(mapState);
};

export const useWorkspaceName = () =>
  useSelector(useCallback(state => selectors.getWorkspaceName(state), []));

export const useWorkspaceId = () => {
  const group = useWorkspaceGroup();
  const name = useWorkspaceName();
  return getId({ group, name });
};

export const useSwitchWorkspaceAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkspaceGroup();
  return useCallback(
    name => dispatch(actions.switchWorkspace.trigger({ group, name })),
    [dispatch, group],
  );
};

export const useAllWorkspaces = () =>
  useSelector(useCallback(state => selectors.getAllWorkspaces(state), []));

export const useWorkspace = () => {
  const id = hooks.useWorkspaceId();
  return useSelector(
    useCallback(state => selectors.getWorkspaceById(state, { id }), [id]),
  );
};

export const useUpdateWorkspaceAction = () => {
  const dispatch = useDispatch();
  const group = hooks.useWorkspaceGroup();
  return useCallback(
    values => dispatch(actions.updateWorkspace.trigger({ ...values, group })),
    [dispatch, group],
  );
};

export const useDiscardWorkspaceChangedSettingsAction = () => {
  const broker = hooks.useBroker();
  const worker = hooks.useWorker();
  const workspace = hooks.useWorkspace();
  const zookeeper = hooks.useZookeeper();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();

  return useCallback(() => {
    updateWorkspace({ ...workspace, broker, worker, zookeeper });
  }, [broker, worker, workspace, zookeeper, updateWorkspace]);
};

export const useShouldBeRestartWorkspace = () => {
  const broker = hooks.useBroker();
  const worker = hooks.useWorker();
  const workspace = hooks.useWorkspace();
  const zookeeper = hooks.useZookeeper();

  const memoizedValue = useMemo(() => {
    const countOfChangedBrokerNodes = workspace?.broker?.nodeNames
      ? size(xor(broker?.nodeNames, workspace.broker.nodeNames))
      : 0;

    const countOfChangedWorkerNodes = workspace?.worker?.nodeNames
      ? size(xor(worker?.nodeNames, workspace.worker.nodeNames))
      : 0;

    const countOfChangedWorkerPlugins = workspace?.worker?.pluginKeys
      ? size(
          xor(
            map(worker?.pluginKeys, key => key.name),
            map(workspace.worker.pluginKeys, key => key.name),
          ),
        )
      : 0;

    const countOfChangedWorkerSharedJars = workspace?.worker?.sharedJarKeys
      ? size(
          xor(
            map(worker?.sharedJarKeys, key => key.name),
            map(workspace.worker.sharedJarKeys, key => key.name),
          ),
        )
      : 0;

    const countOfChangedZookeeperNodes = workspace?.zookeeper?.nodeNames
      ? size(xor(zookeeper?.nodeNames, workspace.zookeeper.nodeNames))
      : 0;

    const shouldBeRestartBroker = countOfChangedBrokerNodes > 0;
    const shouldBeRestartWorker =
      countOfChangedWorkerNodes > 0 ||
      countOfChangedWorkerPlugins > 0 ||
      countOfChangedWorkerSharedJars > 0;
    const shouldBeRestartZookeeper = countOfChangedZookeeperNodes > 0;
    const shouldBeRestartWorkspace =
      shouldBeRestartBroker ||
      shouldBeRestartWorker ||
      shouldBeRestartZookeeper;

    return {
      countOfChangedBrokerNodes,
      countOfChangedWorkerNodes,
      countOfChangedWorkerPlugins,
      countOfChangedWorkerSharedJars,
      countOfChangedZookeeperNodes,
      shouldBeRestartBroker,
      shouldBeRestartWorker,
      shouldBeRestartWorkspace,
      shouldBeRestartZookeeper,
    };
  }, [broker, worker, workspace, zookeeper]);

  return memoizedValue;
};
