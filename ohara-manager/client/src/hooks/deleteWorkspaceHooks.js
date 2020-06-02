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

import * as actions from 'store/actions';
import * as hooks from 'hooks';
import * as selectors from 'store/selectors';
import { getKey } from 'utils/object';

export const useOpenDeleteWorkspaceDialogAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.openDeleteWorkspace.trigger()), [
    dispatch,
  ]);
};

export const useDeleteWorkspace = () =>
  useSelector((state) => state.ui.deleteWorkspace);

export const useIsRollback = () => {
  const getProgress = useMemo(selectors.makeGetDeleteWorkspaceProgress, []);
  return useSelector(
    useCallback((state) => getProgress(state).isRollback, [getProgress]),
  );
};

export const usePauseDeleteWorkspaceAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.pauseDeleteWorkspace.trigger()), [
    dispatch,
  ]);
};

export const useResumeDeleteWorkspaceAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.resumeDeleteWorkspace.trigger()), [
    dispatch,
  ]);
};

export const useRollbackDeleteWorkspaceAction = () => {
  const dispatch = useDispatch();
  return useCallback(
    () => dispatch(actions.rollbackDeleteWorkspace.trigger()),
    [dispatch],
  );
};

export const useDeleteWorkspaceAction = () => {
  const dispatch = useDispatch();

  // Streams, shabondis and connectors are all using the same group
  const group = hooks.useConnectorGroup();

  const zookeeperKey = getKey(hooks.useZookeeper());
  const brokerKey = getKey(hooks.useBroker());
  const workerKey = getKey(hooks.useWorker());
  const workspaceKey = getKey(hooks.useWorkspace());
  const pipelineKeys = hooks.usePipelines().map(getKey);
  const files = hooks.useFiles();
  const connectorKeys = useSelector((state) =>
    selectors.getConnectorByGroup(state, { group }),
  ).map(getKey);
  const shabondiKeys = useSelector((state) =>
    selectors.getShabondisByGroup(state, { group }),
  ).map(getKey);
  const streamKeys = useSelector((state) =>
    selectors.getStreamByGroup(state, { group }),
  ).map(getKey);

  const values = {
    zookeeperKey,
    brokerKey,
    workerKey,
    workspaceKey,
    pipelineKeys,
    connectorKeys,
    shabondiKeys,
    streamKeys,
    files,
  };

  return useCallback(
    (options) =>
      dispatch(
        actions.deleteWorkspace.trigger({
          values,
          options,
        }),
      ),
    [dispatch, values],
  );
};
