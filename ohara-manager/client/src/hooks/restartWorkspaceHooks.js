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

import * as hooks from 'hooks';
import { SERVICE_STATE } from 'api/apiInterface/clusterInterface';
import { KIND } from 'const';

export const useHasRunningServices = () => {
  const connectors = hooks.useConnectors();
  const streams = hooks.useStreams();
  const shabondis = hooks.useShabondis();
  const hasRunning = (service) => service.state === SERVICE_STATE.RUNNING;
  return (
    connectors.some(hasRunning) ||
    streams.some(hasRunning) ||
    shabondis.some(hasRunning)
  );
};

export const useRestartConfirmMessage = (kind) => {
  const workspaceName = hooks.useWorkerName();
  const hasRunningServices = hooks.useHasRunningServices();

  if (hasRunningServices) {
    return (
      <Typography paragraph>
        Oops, there are still some services running in your workspace. You
        should stop all pipelines under this workspace first and then you will
        be able to delete this workspace.
      </Typography>
    );
  } else {
    switch (kind) {
      case KIND.broker:
        return `This action cannot be undone. This will permanently restart the ${workspaceName} and the services under it: brokers, workers and pipelines`;
      case KIND.worker:
        return `This action cannot be undone. This will permanently restart the ${workspaceName} and the services under it: workers and pipelines`;
      default:
        return `This action cannot be undone. This will permanently restart the ${workspaceName} and the services under it: zookeepers, brokers, workers and pipelines`;
    }
  }
};

export const useRefreshWorkspaceAction = (params) => {
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
