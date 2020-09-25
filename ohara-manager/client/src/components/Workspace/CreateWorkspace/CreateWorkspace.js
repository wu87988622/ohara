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

import React, { useRef, useState } from 'react';
import { capitalize, isEmpty, compact, flatten } from 'lodash';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';

import { Form } from 'const';
import * as hooks from 'hooks';
import FullScreenDialog from 'components/common/Dialog/FullScreenDialog';
import Stepper from 'components/common/FSMStepper';
import CreateWorkspaceForm from './CreateWorkspaceForm';
import { WorkspaceFlag } from 'api/apiInterface/workspaceInterface';

export default () => {
  const isDialogOpen = hooks.useIsCreateWorkspaceDialogOpen();
  const closeDialog = hooks.useCloseCreateWorkspaceDialogAction();
  const mode = hooks.useCreateWorkspaceMode();
  const createWorkspace = hooks.useCreateWorkspaceAction();
  const createWorker = hooks.useCreateWorkerAction();
  const createBroker = hooks.useCreateBrokerAction();
  const createZookeeper = hooks.useCreateZookeeperAction();
  const createVolume = hooks.useCreateVolumeAction();
  const startBroker = hooks.useStartBrokerAction();
  const startWorker = hooks.useStartWorkerAction();
  const startZookeeper = hooks.useStartZookeeperAction();
  const startVolume = hooks.useStartVolumeAction();
  const stopBroker = hooks.useStopBrokerAction();
  const stopWorker = hooks.useStopWorkerAction();
  const stopZookeeper = hooks.useStopZookeeperAction();
  const stopVolume = hooks.useStopVolumeAction();
  const deleteBroker = hooks.useDeleteBrokerAction();
  const deleteWorker = hooks.useDeleteWorkerAction();
  const deleteZookeeper = hooks.useDeleteZookeeperAction();
  const deleteVolume = hooks.useDeleteVolumeAction();
  const deleteWorkspace = hooks.useDeleteWorkspaceAction();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();
  const switchWorkspace = hooks.useSwitchWorkspaceAction();
  const refreshNodes = hooks.useFetchNodesAction();
  const eventLog = hooks.useEventLog();
  const introDialog = hooks.useIntroDialog();
  const switchFormStep = hooks.useSwitchCreateWorkspaceStepAction();
  const resetForm = hooks.useReduxFormResetAction();
  const clearVolumes = hooks.useClearVolumesAction();

  const [submitting, setSubmitting] = useState(false);
  const [steps, setSteps] = useState([]);
  const [workspaceName, setWorkspaceName] = useState(null);
  const stepperRef = useRef(null);

  const handleSubmit = (values) => {
    const volume = () => {
      if (values.zkVolume && values.bkVolume) {
        return [
          {
            name: 'create zookeeper volume',
            action: () => createVolume(values?.zkVolume),
            revertAction: () => deleteVolume(values?.zkVolume),
          },
          {
            name: 'start zookeeper volume',
            action: () => startVolume(values?.zkVolume),
            revertAction: () => stopVolume(values?.zkVolume),
          },
          {
            name: 'create broker volume',
            action: () => createVolume(values?.bkVolume),
            revertAction: () => deleteVolume(values?.bkVolume),
          },
          {
            name: 'start broker volume',
            action: () => startVolume(values?.bkVolume),
            revertAction: () => stopVolume(values?.bkVolume),
          },
        ];
      } else {
        return null;
      }
    };

    setWorkspaceName(values?.name);
    setSteps(
      flatten(
        compact([
          {
            name: 'create workspace',
            action: () =>
              createWorkspace({ ...values, flag: WorkspaceFlag.CREATING }),
            revertAction: () => deleteWorkspace(values?.name),
          },
          volume(),
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
            action: async () => {
              await updateWorkspace({
                name: values.name,
                flag: WorkspaceFlag.CREATED,
              });
              // Log a success message to Event Log
              eventLog.info(`Successfully created workspace ${values?.name}.`);
              // Clear volume
              clearVolumes();
              // Clear form data
              resetForm(Form.CREATE_WORKSPACE);
              // Back to the first page of the form
              switchFormStep(0);
              // Close all dialogs
              introDialog.close();
              // Switch to the workspace you just created
              switchWorkspace(values?.name);
              // Refetch node list after creation successfully in order to get the runtime data
              refreshNodes();
            },
          },
        ]),
      ),
    );
    setSubmitting(true);
  };

  const handleClose = () => {
    const isFinish = stepperRef.current?.isFinish();
    const errors = stepperRef.current?.getErrorLogs();
    if (!isFinish && !isEmpty(errors)) {
      eventLog.error({
        title: `Failed to create workspace ${workspaceName}.`,
        data: errors,
      });
    }
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
          data-testid="create-workspace-progress-dialog"
          fullWidth={submitting}
          maxWidth={'sm'}
          open={submitting}
        >
          <DialogTitle id="alert-dialog-title">Create Workspace</DialogTitle>
          <DialogContent>
            <Stepper
              onClose={handleClose}
              ref={stepperRef}
              revertible
              revertText="Cancel"
              steps={steps}
            />
          </DialogContent>
        </Dialog>
      )}
    </FullScreenDialog>
  );
};
