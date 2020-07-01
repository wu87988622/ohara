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

import React, { useState } from 'react';
import { capitalize } from 'lodash';
import { reset } from 'redux-form';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';

import { FORM } from 'const';
import * as hooks from 'hooks';
import FullScreenDialog from 'components/common/Dialog/FullScreenDialog';
import Stepper from 'components/common/FSMStepper';
import CreateWorkspaceForm from './CreateWorkspaceForm';

export default () => {
  const isDialogOpen = hooks.useIsCreateWorkspaceDialogOpen();
  const closeDialog = hooks.useCloseCreateWorkspaceDialogAction();
  const mode = hooks.useCreateWorkspaceMode();
  const createWorkspace = hooks.useCreateWorkspaceAction();
  const createWorker = hooks.useCreateWorkerAction();
  const createBroker = hooks.useCreateBrokerAction();
  const createZookeeper = hooks.useCreateZookeeperAction();
  const startBroker = hooks.useStartBrokerAction();
  const startWorker = hooks.useStartWorkerAction();
  const startZookeeper = hooks.useStartZookeeperAction();
  const stopBroker = hooks.useStopBrokerAction();
  const stopWorker = hooks.useStopWorkerAction();
  const stopZookeeper = hooks.useStopZookeeperAction();
  const deleteBroker = hooks.useDeleteBrokerAction();
  const deleteWorker = hooks.useDeleteWorkerAction();
  const deleteZookeeper = hooks.useDeleteZookeeperAction();
  const deleteWorkspace = hooks.useDeleteWorkspaceAction();
  const switchWorkspace = hooks.useSwitchWorkspaceAction();
  const refreshNodes = hooks.useFetchNodesAction();
  const eventLog = hooks.useEventLog();
  const closeIntroDialog = hooks.useCloseIntroAction();
  const switchFormStep = hooks.useSwitchCreateWorkspaceStepAction();

  const [submitting, setSubmitting] = useState(false);
  const [steps, setSteps] = useState([]);

  const handleSubmit = (values) => {
    setSteps([
      {
        name: 'create workspace',
        action: () => createWorkspace(values),
        revertAction: () => deleteWorkspace(values?.name),
      },
      {
        name: 'create zookeeper',
        action: () => createZookeeper(values?.zookeeper),
        revertAction: () => deleteZookeeper(values?.zookeeper?.name),
      },
      {
        name: 'create broker',
        action: () => createBroker(values?.broker),
        revertAction: () => deleteBroker(values?.broker?.name),
      },
      {
        name: 'create worker',
        action: () => createWorker(values?.worker),
        revertAction: () => deleteWorker(values?.worker?.name),
      },
      {
        name: 'start zookeeper',
        action: () => startZookeeper(values?.zookeeper?.name),
        revertAction: () => stopZookeeper(values?.zookeeper?.name),
      },
      {
        name: 'start broker',
        action: () => startBroker(values?.broker?.name),
        revertAction: () => stopBroker(values?.broker?.name),
      },
      {
        name: 'start worker',
        action: () => startWorker(values?.worker?.name),
        revertAction: () => stopWorker(values?.worker?.name),
      },
      {
        name: 'finalize',
        action: () => {
          return new Promise((resolve) => {
            // Log a success message to Event Log
            eventLog.info(`Successfully created workspace ${values?.name}.`);
            // Clear form data
            reset(FORM.CREATE_WORKSPACE);
            // Back to the first page of the form
            switchFormStep(0);
            // Close all dialogs
            closeIntroDialog();
            // Switch to the workspace you just created
            switchWorkspace(values?.name);
            // Refetch node list after creation successfully in order to get the runtime data
            refreshNodes();
            resolve();
          });
        },
      },
    ]);
    setSubmitting(true);
  };

  const handleClose = () => {
    setSubmitting(false);
    closeDialog();
  };

  return (
    <FullScreenDialog
      onClose={closeDialog}
      open={isDialogOpen}
      title={`Create workspace - ${capitalize(mode)}`}
    >
      <CreateWorkspaceForm onCancel={closeDialog} onSubmit={handleSubmit} />
      {submitting && (
        <Dialog
          data-testid="create-workspace"
          fullWidth={submitting}
          maxWidth={'md'}
          onClose={handleClose}
          open={submitting}
        >
          <DialogTitle id="alert-dialog-title">
            {'Create Workspace'}
          </DialogTitle>
          <DialogContent>
            <Stepper onClose={handleClose} revertible steps={steps} />
          </DialogContent>
        </Dialog>
      )}
    </FullScreenDialog>
  );
};
