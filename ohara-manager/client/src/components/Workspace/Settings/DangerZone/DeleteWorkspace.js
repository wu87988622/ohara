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

import React, { useRef } from 'react';
import PropTypes from 'prop-types';
import { isEmpty } from 'lodash';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';

import { WORKSPACE_FLAGS } from 'api/apiInterface/workspaceInterface';
import Stepper from 'components/common/FSMStepper';
import * as hooks from 'hooks';

const DeleteWorkspace = (props) => {
  const { isOpen, onClose } = props;
  const workspace = hooks.useWorkspace();

  const stopBroker = hooks.useStopBrokerAction();
  const stopTopics = hooks.useStopTopicsInWorkspaceAction();
  const stopWorker = hooks.useStopWorkerAction();
  const stopZookeeper = hooks.useStopZookeeperAction();

  const deleteBroker = hooks.useDeleteBrokerAction();
  const deleteConnectors = hooks.useDeleteConnectorsInWorkspaceAction();
  const deleteFiles = hooks.useDeleteFilesInWorkspaceAction();
  const deletePipelines = hooks.useDeletePipelinesInWorkspaceAction();
  const deleteShabondis = hooks.useDeleteShabondisInWorkspaceAction();
  const deleteStreams = hooks.useDeleteStreamsInWorkspaceAction();
  const deleteTopics = hooks.useDeleteTopicsInWorkspaceAction();
  const deleteWorker = hooks.useDeleteWorkerAction();
  const deleteWorkspace = hooks.useDeleteWorkspaceAction();
  const deleteZookeeper = hooks.useDeleteZookeeperAction();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();

  const eventLog = hooks.useEventLog();
  const stepperRef = useRef(null);
  const steps = [
    {
      name: 'prepare',
      action: () =>
        updateWorkspace({ ...workspace, flag: WORKSPACE_FLAGS.DELETING }),
    },
    {
      name: 'delete connectors',
      action: () => deleteConnectors(),
    },
    {
      name: 'delete streams',
      action: () => deleteStreams(),
    },
    {
      name: 'delete shabondis',
      action: () => deleteShabondis(),
    },
    {
      name: 'stop topics',
      action: () => stopTopics(),
    },
    {
      name: 'delete topics',
      action: () => deleteTopics(),
    },
    {
      name: 'stop worker',
      action: () => stopWorker(workspace?.worker?.name),
    },
    {
      name: 'delete worker',
      action: () => deleteWorker(workspace?.worker?.name),
    },
    {
      name: 'stop broker',
      action: () => stopBroker(workspace?.broker?.name),
    },
    {
      name: 'delete broker',
      action: () => deleteBroker(workspace?.broker?.name),
    },
    {
      name: 'stop zookeeper',
      action: () => stopZookeeper(workspace?.zookeeper?.name),
    },
    {
      name: 'delete zookeeper',
      action: () => deleteZookeeper(workspace?.zookeeper?.name),
    },
    {
      name: 'delete pipelines',
      action: () => deletePipelines(),
    },
    {
      name: 'delete files',
      action: () => deleteFiles(),
    },
    {
      name: 'delete workspace',
      action: () => deleteWorkspace(),
    },
  ];

  const handleClose = () => {
    const isFinish = stepperRef.current.isFinish();
    const errors = stepperRef.current.getErrorLogs();
    if (!isFinish && !isEmpty(errors)) {
      eventLog.error({
        title: `Failed to delete workspace ${workspace?.name}.`,
        data: errors,
      });
    }
    if (onClose) onClose();
  };

  return (
    <Dialog
      data-testid="delete-workspace"
      fullWidth={isOpen}
      maxWidth={'md'}
      open={isOpen}
    >
      <DialogTitle>{'Delete Workspace'}</DialogTitle>
      <DialogContent>
        <Stepper onClose={handleClose} ref={stepperRef} steps={steps} />
      </DialogContent>
    </Dialog>
  );
};

DeleteWorkspace.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
};

DeleteWorkspace.defaultProps = {
  onClose: () => {},
};

export default DeleteWorkspace;
