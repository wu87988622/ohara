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
import { capitalize } from 'lodash';

import * as hooks from 'hooks';
import FullScreenDialog from 'components/common/Dialog/FullScreenDialog';
import { Progress } from 'components/common/Progress';
import CreateWorkspaceForm from './CreateWorkspaceForm';

export default () => {
  const isDialogOpen = hooks.useIsCreateWorkspaceDialogOpen();
  const progress = hooks.useCreateWorkspaceProgress();
  const closeDialog = hooks.useCloseCreateWorkspaceDialogAction();
  const createWorkspace = hooks.useCreateWorkspaceAction();
  const mode = hooks.useCreateWorkspaceMode();

  return (
    <FullScreenDialog
      title={`Create workspace - ${capitalize(mode)}`}
      open={isDialogOpen}
      handleClose={closeDialog}
    >
      <CreateWorkspaceForm onCancel={closeDialog} onSubmit={createWorkspace} />
      <Progress
        open={progress.open}
        steps={progress.steps}
        createTitle={'Create Workspace'}
        activeStep={progress.activeStep}
      />
    </FullScreenDialog>
  );
};
