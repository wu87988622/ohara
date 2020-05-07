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

const DeleteWorkspace = () => {
  const pause = hooks.usePauseDeleteWorkspaceAction();
  const resume = hooks.useResumeDeleteWorkspaceAction();
  const close = hooks.useCloseDeleteWorkspaceDialogAction();
  const rollback = hooks.useRollbackDeleteWorkspaceAction();
  const autoClose = hooks.useAutoCloseDeleteWorkspaceDialogAction();
  const deleteWorkspace = hooks.useDeleteWorkspaceAction();
  const workspaceId = hooks.useWorkspaceId();
  const zookeeperId = hooks.useZookeeperId();
  const brokerId = hooks.useBrokerId();
  const workerId = hooks.useWorkerId();
  const workspace = hooks.useWorkspace();

  const currentDeleteWorkspace = hooks.useDeleteWorkspace();
  const {
    isOpen,
    skipList,
    isAutoClose,
    closeDisable,
  } = currentDeleteWorkspace;
  const {
    steps,
    activeStep,
    message,
    isPause,
    log,
  } = currentDeleteWorkspace.progress;

  return (
    <LogProgress
      createTitle={'Delete Workspace'}
      isOpen={isOpen}
      steps={steps}
      activeStep={activeStep}
      message={message}
      onPause={pause}
      isPause={isPause}
      data={log}
      onResume={() => {
        resume();
        deleteWorkspace({
          workspace: convertIdToKey(workspaceId),
          zookeeper: convertIdToKey(zookeeperId),
          broker: convertIdToKey(brokerId),
          worker: convertIdToKey(workerId),
          skipList,
        });
      }}
      onRollback={() => {
        rollback();
        deleteWorkspace({
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
      onResetClusters={() => {}}
      isAutoClose={isAutoClose}
      closeDisable={closeDisable}
    />
  );
};

export default DeleteWorkspace;
