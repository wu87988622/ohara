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

import React, { useEffect, useRef } from 'react';
import SplitPane from 'react-split-pane';
import { useParams } from 'react-router-dom';
import { isEmpty } from 'lodash';

import * as hooks from 'hooks';
import Pipeline from 'components/Pipeline';
import EventLog from 'components/EventLog';
import { DevToolDialog } from 'components/DevTool';
import {
  WorkspaceList as ListWorkspacesDialog,
  CreateWorkspace as CreateWorkspaceDialog,
} from 'components/Workspace';
import DeleteWorkspaceDialog from 'components/Workspace/Settings/DangerZone/DeleteWorkspace';
import IntroDialog from 'components/Intro';
import StyledSnackbar from 'components/common/Snackbar';
import Navigator from './Navigator';
import AppBar from './AppBar';
import { Wrapper } from './AppLayoutStyles';

const AppLayout = () => {
  const { workspaceName, pipelineName } = useParams();
  const isAppReady = hooks.useIsAppReady();
  const workspaces = hooks.useAllWorkspaces();
  const introDialog = hooks.useIntroDialog();
  const eventLogDialog = hooks.useEventLogDialog();
  const devToolDialog = hooks.useDevToolDialog();
  const workspaceDeleteDialog = hooks.useWorkspaceDeleteDialog();
  const pipelineApiRef = useRef(null);

  // Initialize the application
  hooks.useInitializeApp(workspaceName, pipelineName);

  // The introduction dialog will be displayed on first used
  useEffect(() => {
    const hasIntroDialogBeenOpened = !!introDialog?.lastUpdated;
    if (isAppReady && !hasIntroDialogBeenOpened) {
      if (isEmpty(workspaces)) introDialog.open();
      // If there is any workspace, the intro dialog will not be displayed.
      // The call to close the introDialog will assign a timestamp to 'lastUpdated'.
      else introDialog.close();
    }
  }, [isAppReady, workspaces, introDialog]);

  return (
    <Wrapper>
      <SplitPane allowResize={false} defaultSize={64} split="vertical">
        <AppBar />
        <SplitPane
          defaultSize={devToolDialog.isOpen || eventLogDialog.isOpen ? 240 : 0}
          maxSize={devToolDialog.isOpen || eventLogDialog.isOpen ? 480 : 0}
          minSize={devToolDialog.isOpen || eventLogDialog.isOpen ? 80 : 0}
          primary="second"
          split="horizontal"
        >
          <SplitPane
            defaultSize={240}
            maxSize={320}
            minSize={160}
            split="vertical"
          >
            <Navigator pipelineApi={pipelineApiRef.current} />
            <Pipeline ref={pipelineApiRef} />
          </SplitPane>
          {eventLogDialog.isOpen && <EventLog />}
          {devToolDialog.isOpen && <DevToolDialog />}
          <div />
        </SplitPane>
      </SplitPane>

      <IntroDialog isOpen={introDialog.isOpen} onClose={introDialog.close} />
      <CreateWorkspaceDialog />
      <ListWorkspacesDialog />
      <DeleteWorkspaceDialog
        isOpen={workspaceDeleteDialog.isOpen}
        onClose={workspaceDeleteDialog.close}
      />

      <StyledSnackbar />
    </Wrapper>
  );
};

export default AppLayout;
