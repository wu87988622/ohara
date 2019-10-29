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
import { useHistory, useParams } from 'react-router-dom';

import { useSnackbar } from 'context/SnackbarContext';
import { useWorkspace } from 'context/WorkspaceConetxt';
import { Button } from 'components/common/Form';

const Workspace = () => {
  const showMessage = useSnackbar();
  const history = useHistory();
  const { workspaces } = useWorkspace();
  const { workspaceName } = useParams();

  const hasWorkspace = workspaces.length > 0;

  let currentWorkspace;
  if (workspaceName) {
    const current = workspaces.find(
      workspace => workspace.settings.name === workspaceName,
    );

    // If the `current` workspace is found in the workspace list
    if (current) currentWorkspace = current;

    // If the `current` workspace is not found in the list but the
    // list is not empty, let's display the first workspace
    if (current === undefined && hasWorkspace) {
      history.push(`/${workspaces[0].settings.name}`);
    }
  } else if (hasWorkspace) {
    // Display the first workspace as the fallback
    history.push(`/${workspaces[0].settings.name}`);
  }

  return currentWorkspace ? (
    <>
      <h1>Workerspace name: {currentWorkspace.settings.name}</h1>
      <Button onClick={() => showMessage(currentWorkspace.settings.name)}>
        Show me the name
      </Button>
    </>
  ) : (
    <h1>You don't have any workspace yet!</h1>
  );
};

export default Workspace;
