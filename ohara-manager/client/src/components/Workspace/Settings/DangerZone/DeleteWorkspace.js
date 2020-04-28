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
import { getKeyWithId } from 'utils/object';

const DeleteWorkspace = () => {
  const onPause = hooks.usePauseDeleteWorkspaceAction();
  const onResume = hooks.useResumeDeleteWorkspaceAction();
  const onRollback = hooks.useRollbackDeleteWorkspaceAction();
  const deleteWorkspace = hooks.useDeleteWorkspaceAction();
  const workspaceId = hooks.useWorkspaceId();
  const zookeeperId = hooks.useZookeeperId();
  const brokerId = hooks.useBrokerId();
  const workerId = hooks.useWorkerId();
  const workspace = hooks.useWorkspace();

  const currentDeleteWorkspace = hooks.useDeleteWorkspace();
  const { isOpen, skipList } = currentDeleteWorkspace;
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
      onPause={onPause}
      isPause={isPause}
      data={log}
      onResume={() => {
        onResume();
        deleteWorkspace({
          workspace: getKeyWithId(workspaceId),
          zookeeper: getKeyWithId(zookeeperId),
          broker: getKeyWithId(brokerId),
          worker: getKeyWithId(workerId),
          skipList,
        });
      }}
      rollback={() => {
        onRollback();
        deleteWorkspace({
          workspace: getKeyWithId(workspaceId),
          zookeeper: getKeyWithId(zookeeperId),
          broker: getKeyWithId(brokerId),
          worker: getKeyWithId(workerId),
          skipList,
          isRollBack: true,
          tmpWorker: workspace.worker,
          tmpBroker: workspace.Broker,
          tmpZookeeper: workspace.Zookeeper,
        });
      }}
    />
  );
};

export default DeleteWorkspace;
