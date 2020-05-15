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

import React from 'react';
import LogProgress from 'components/common/Progress/LogProgress';

import * as hooks from 'hooks';
import { convertIdToKey } from 'utils/object';

const RestartWorkspace = () => {
  const brokerName = hooks.useBrokerName();
  const workerName = hooks.useWorkerName();
  const zookeeperName = hooks.useZookeeperName();
  const pause = hooks.usePauseRestartWorkspaceAction();
  const close = hooks.useCloseRestartWorkspaceDialogAction();
  const resume = hooks.useResumeRestartWorkspaceAction();
  const rollback = hooks.useRollbackRestartWorkspaceAction();
  const autoClose = hooks.useAutoCloseRestartWorkspaceDialogAction();
  const startWorkspace = hooks.useRestartWorkspaceAction();
  const resetClusters = hooks.useRefreshWorkspaceAction({
    zkName: zookeeperName,
    bkName: brokerName,
    wkName: workerName,
  });
  const workspaceId = hooks.useWorkspaceId();
  const zookeeperId = hooks.useZookeeperId();
  const brokerId = hooks.useBrokerId();
  const workerId = hooks.useWorkerId();
  const workspace = hooks.useWorkspace();

  const currentRestartWorkspace = hooks.useRestartWorkspace();
  const {
    isOpen,
    skipList,
    isAutoClose,
    closeDisable,
  } = currentRestartWorkspace;
  const {
    steps,
    activeStep,
    message,
    isPause,
    log,
  } = currentRestartWorkspace.progress;

  return (
    <LogProgress
      createTitle={'Restart Workspace'}
      isOpen={isOpen}
      steps={steps}
      activeStep={activeStep}
      message={message}
      onPause={pause}
      isPause={isPause}
      data={log}
      onResume={() => {
        resume();
        startWorkspace({
          workspace: convertIdToKey(workspaceId),
          zookeeper: convertIdToKey(zookeeperId),
          broker: convertIdToKey(brokerId),
          worker: convertIdToKey(workerId),
          skipList,
        });
      }}
      onRollback={() => {
        rollback();
        startWorkspace({
          workspace: convertIdToKey(workspaceId),
          zookeeper: convertIdToKey(zookeeperId),
          broker: convertIdToKey(brokerId),
          worker: convertIdToKey(workerId),
          skipList,
          isRollBack: true,
          tmpWorker: workspace.worker,
          tmpBroker: workspace.Broker,
          tmpZookeeper: workspace.Zookeeper,
        });
      }}
      onClose={() => close()}
      onAutoClose={() => autoClose()}
      onResetClusters={() => resetClusters()}
      isAutoClose={isAutoClose}
      closeDisable={closeDisable}
    />
  );
};

export default RestartWorkspace;
