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
import PropTypes from 'prop-types';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';

import { WorkspaceFlag } from 'api/apiInterface/workspaceInterface';
import Stepper from 'components/common/FSMStepper';
import * as hooks from 'hooks';
import { KIND } from 'const';
import { omit } from 'lodash';

const RestartWorkspace = (props) => {
  const { isOpen, onClose, restartService } = props;

  const eventLog = hooks.useEventLog();
  const startBrokerAction = hooks.useStartBrokerAction();
  const startTopicsAction = hooks.useStartTopicsInWorkspaceAction();
  const startWorkerAction = hooks.useStartWorkerAction();
  const startZookeeperAction = hooks.useStartZookeeperAction();
  const stopBrokerAction = hooks.useStopBrokerAction();
  const stopTopicsAction = hooks.useStopTopicsInWorkspaceAction();
  const stopWorkerAction = hooks.useStopWorkerAction();
  const stopZookeeperAction = hooks.useStopZookeeperAction();
  const updateBrokerAction = hooks.useUpdateBrokerAction();
  const updateWorkerAction = hooks.useUpdateWorkerAction();
  const updateZookeeperAction = hooks.useUpdateZookeeperAction();
  const refreshZookeeperAction = hooks.useFetchZookeeperAction();
  const refreshBrokerAction = hooks.useFetchBrokerAction();
  const refreshWorkerAction = hooks.useFetchWorkerAction();
  const refreshNodeAction = hooks.useFetchNodesAction();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();

  const worker = hooks.useWorker();
  const broker = hooks.useBroker();
  const zookeeper = hooks.useZookeeper();
  const workspace = hooks.useWorkspace();

  const getSteps = (restartService) => {
    const prepare = {
      name: 'prepare',
      action: () =>
        updateWorkspace({ ...workspace, flag: WorkspaceFlag.RESTARTING }),
      revertAction: () =>
        updateWorkspace({ ...workspace, flag: WorkspaceFlag.RESTARTED }),
    };

    const stopWorker = {
      name: 'stop worker',
      action: () => stopWorkerAction(worker.name),
      revertAction: () => startWorkerAction(worker.name),
    };
    const updateWorker = {
      name: 'update worker',
      action: () =>
        updateWorkerAction({
          ...workspace.worker,
          tags: omit(worker, ['tags']),
        }),
      revertAction: () => updateWorkerAction({ ...worker }),
    };
    const stopTopic = {
      name: 'stop topic',
      action: () => stopTopicsAction(),
      revertAction: () => startTopicsAction(),
    };
    const stopBroker = {
      name: 'stop broker',
      action: () => stopBrokerAction(broker.name),
      revertAction: () => startBrokerAction(broker.name),
    };
    const updateBroker = {
      name: 'update broker',
      action: () =>
        updateBrokerAction({
          ...workspace.broker,
          tags: omit(broker, ['tags']),
        }),
      revertAction: () => updateBrokerAction({ ...broker }),
    };
    const stopZookeeper = {
      name: 'stop zookeeper',
      action: () => stopZookeeperAction(zookeeper.name),
      revertAction: () => startZookeeperAction(zookeeper.name),
    };
    const updateZookeeper = {
      name: 'update zookeeper',
      action: () =>
        updateZookeeperAction({
          ...workspace.zookeeper,
          tags: omit(zookeeper, ['tags']),
        }),
      revertAction: () => updateZookeeperAction({ ...zookeeper }),
    };
    const startZookeeper = {
      name: 'start zookeeper',
      action: () => startZookeeperAction(zookeeper.name),
      revertAction: () => stopZookeeperAction(zookeeper.name),
    };
    const startBroker = {
      name: 'start broker',
      action: () => startBrokerAction(broker.name),
      revertAction: () => stopBrokerAction(broker.name),
    };
    const startTopic = {
      name: 'start topic',
      action: () => startTopicsAction(),
      revertAction: () => stopTopicsAction(),
    };
    const startWorker = {
      name: 'start worker',
      action: () => startWorkerAction(worker.name),
    };
    const restartWorkspace = {
      name: 'restart workspace',
      action: () => {
        return new Promise((resolve) => {
          // Log a success message to Event Log
          eventLog.info(`Successfully Restart workspace ${workspace.name}.`);

          updateWorkspace({ ...workspace, flag: WorkspaceFlag.RESTARTED });

          refreshZookeeperAction(zookeeper.name);

          refreshBrokerAction(broker.name);

          refreshWorkerAction(worker.name);

          refreshNodeAction();

          resolve();
        });
      },
    };

    switch (restartService) {
      //Restart target is worker
      case KIND.worker:
        return [
          prepare,
          stopWorker,
          updateWorker,
          startWorker,
          restartWorkspace,
        ];

      //Restart target is worker and broker
      case KIND.broker:
        return [
          prepare,
          stopWorker,
          updateWorker,
          stopTopic,
          stopBroker,
          updateBroker,
          startBroker,
          startTopic,
          startWorker,
          restartWorkspace,
        ];

      default:
        return [
          prepare,
          stopWorker,
          updateWorker,
          stopTopic,
          stopBroker,
          updateBroker,
          stopZookeeper,
          updateZookeeper,
          startZookeeper,
          startBroker,
          startTopic,
          startWorker,
          restartWorkspace,
        ];
    }
  };

  return (
    <Dialog
      data-testid="restart-workspace-progress-dialog"
      fullWidth={isOpen}
      maxWidth="sm"
      open={isOpen}
    >
      <DialogTitle>Restart Workspace</DialogTitle>
      <DialogContent>
        <Stepper
          onClose={onClose}
          revertible
          steps={getSteps(restartService)}
        />
      </DialogContent>
    </Dialog>
  );
};

RestartWorkspace.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
  restartService: PropTypes.string.isRequired,
};

RestartWorkspace.defaultProps = {
  onClose: () => {},
};

export default RestartWorkspace;
