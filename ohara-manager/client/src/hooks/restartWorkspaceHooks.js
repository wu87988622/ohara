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

import React, { useCallback } from 'react';
import Typography from '@material-ui/core/Typography';
import { useDispatch, useSelector } from 'react-redux';

import * as actions from 'store/actions';
import * as hooks from 'hooks';
import { convertIdToKey } from 'utils/object';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';

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
  const workspace = hooks.useWorkspace();
  const workspaceId = hooks.useWorkspaceId();
  const zookeeperId = hooks.useZookeeperId();
  const brokerId = hooks.useBrokerId();
  const workerId = hooks.useWorkerId();
  const tmpWorker = hooks.useWorker();
  const tmpBroker = hooks.useBroker();
  const tmpZookeeper = hooks.useZookeeper();
  const topics = hooks.useTopicsInWorkspace();

  const values = {
    workspace: convertIdToKey(workspaceId),
    zookeeper: convertIdToKey(zookeeperId),
    broker: convertIdToKey(brokerId),
    worker: convertIdToKey(workerId),
    workerSettings: workspace.worker,
    brokerSettings: workspace.broker,
    zookeeperSettings: workspace.zookeeper,
    tmpWorker,
    tmpBroker,
    tmpZookeeper,
    topics,
  };

  return useCallback(
    option => dispatch(actions.restartWorkspace.trigger({ values, option })),
    [dispatch, values],
  );
};

export const useHasRunningServices = () => {
  const connectors = hooks.useConnectors();
  const streams = hooks.useStreams();
  const shabondis = hooks.useShabondis();
  const hasRunning = service => service.state === SERVICE_STATE.RUNNING;
  return (
    connectors.some(hasRunning) ||
    streams.some(hasRunning) ||
    shabondis.some(hasRunning)
  );
};

export const useRestartConfirmMessage = () => {
  const workspaceName = hooks.useWorkerName();
  const hasRunningServices = hooks.useHasRunningServices();

  return hasRunningServices ? (
    <Typography paragraph>
      Oops, there are still some services running in your workspace. You should
      stop all pipelines under this workspace first and then you will be able to
      delete this workspace.
    </Typography>
  ) : (
    `This action cannot be undone. This will permanently restart the ${workspaceName} and the services under it: zookeepers, brokers, workers and pipelines`
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
