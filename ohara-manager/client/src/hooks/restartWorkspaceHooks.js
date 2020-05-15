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

import { useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import * as actions from 'store/actions';
import * as hooks from 'hooks';

export const useOpenRestartWorkspaceDialogAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.openRestartWorkspace.trigger()), [
    dispatch,
  ]);
};

export const useCloseRestartWorkspaceDialogAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.closeRestartWorkspace.trigger()), [
    dispatch,
  ]);
};

export const useAutoCloseRestartWorkspaceDialogAction = () => {
  const dispatch = useDispatch();
  return useCallback(
    () => dispatch(actions.autoCloseRestartWorkspace.trigger()),
    [dispatch],
  );
};

export const useRestartWorkspace = () =>
  useSelector(state => state.ui.restartWorkspace);

export const usePauseRestartWorkspaceAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.pauseRestartWorkspace.trigger()), [
    dispatch,
  ]);
};

export const useResumeRestartWorkspaceAction = () => {
  const dispatch = useDispatch();
  return useCallback(() => dispatch(actions.resumeRestartWorkspace.trigger()), [
    dispatch,
  ]);
};

export const useRollbackRestartWorkspaceAction = () => {
  const dispatch = useDispatch();
  return useCallback(
    () => dispatch(actions.rollbackRestartWorkspace.trigger()),
    [dispatch],
  );
};

export const useRestartWorkspaceAction = () => {
  const dispatch = useDispatch();
  return useCallback(
    values => dispatch(actions.restartWorkspace.trigger(values)),
    [dispatch],
  );
};

export const useRefreshWorkspaceAction = params => {
  const fetchZookeeper = hooks.useFetchZookeeperAction();
  const fetchBroker = hooks.useFetchBrokerAction();
  const fetchWorker = hooks.useFetchWorkerAction();
  return useCallback(() => {
    fetchZookeeper(params.zkName);
    fetchBroker(params.bkName);
    fetchWorker(params.wkName);
  }, [
    fetchBroker,
    fetchWorker,
    fetchZookeeper,
    params.bkName,
    params.wkName,
    params.zkName,
  ]);
};
