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
import { useParams, Link, NavLink } from 'react-router-dom';
import Tooltip from '@material-ui/core/Tooltip';
import AppsIcon from '@material-ui/icons/Apps';
import DeveloperModeIcon from '@material-ui/icons/DeveloperMode';
import AssignmentIcon from '@material-ui/icons/Assignment';
import StorageIcon from '@material-ui/icons/Storage';
import AddIcon from '@material-ui/icons/Add';
import IconButton from '@material-ui/core/IconButton';

import * as context from 'context';
import { useNewWorkspace } from 'context/NewWorkspaceContext';
import { useNodeDialog } from 'context/NodeDialogContext';
import { WorkspaceList as ListWorkspacesDialog } from 'components/Workspace';
import { usePrevious } from 'utils/hooks';
import { ReactComponent as Logo } from 'images/logo.svg';
import { StyledAppBar } from './AppBarStyles';

// Since Mui doesn't provide a vertical AppBar, we're creating our own
// therefore, this AppBar has nothing to do with Muis
const AppBar = () => {
  const { workspaceName, pipelineName } = useParams();
  const { setWorkspaceName, setPipelineName } = context.useApp();
  const { workspaces } = context.useWorkspace();
  const { setIsOpen: setIsNewWorkspaceOpen } = useNewWorkspace();
  const { setIsOpen: setIsNodeDialogOpen } = useNodeDialog();
  const {
    open: openDevToolDialog,
    isOpen: isDevToolDialogOpen,
    close: closeDevToolDialog,
  } = context.useDevToolDialog();
  const { open: openListWorkspacesDialog } = context.useListWorkspacesDialog();

  const prevWorkspaceName = usePrevious(workspaceName);
  const prevPipelineName = usePrevious(pipelineName);

  React.useEffect(() => {
    if (workspaceName && workspaceName !== prevWorkspaceName) {
      setWorkspaceName(workspaceName);
    }
  }, [prevWorkspaceName, setWorkspaceName, workspaceName]);

  React.useEffect(() => {
    if (pipelineName && pipelineName !== prevPipelineName) {
      setPipelineName(pipelineName);
    }
  }, [pipelineName, prevPipelineName, setPipelineName]);

  return (
    <StyledAppBar>
      <header>
        <div className="brand">
          <Link to="/">
            <Logo width="38" height="38" />
          </Link>
        </div>
        <div className="workspace-list">
          {workspaces.map(({ name }) => {
            const displayName = name.substring(0, 2).toUpperCase();

            return (
              <Tooltip
                key={name}
                title={name}
                placement="right"
                enterDelay={1000}
              >
                <NavLink
                  activeClassName="active-link"
                  className="workspace-name item"
                  to={`/${name}`}
                >
                  {displayName}
                </NavLink>
              </Tooltip>
            );
          })}

          <Tooltip
            title="Create a new workspace"
            placement="right"
            enterDelay={1000}
          >
            <div
              className="add-workspace item"
              onClick={() => setIsNewWorkspaceOpen(true)}
            >
              <AddIcon />
            </div>
          </Tooltip>
        </div>

        <div className="tools">
          <Tooltip title="Workspace list" placement="right" enterDelay={1000}>
            <IconButton
              className="workspace item"
              onClick={openListWorkspacesDialog}
            >
              <AppsIcon />
            </IconButton>
          </Tooltip>

          <IconButton disabled className="item">
            <AssignmentIcon />
          </IconButton>

          <Tooltip title="Developer Tools" placement="right" enterDelay={1000}>
            <IconButton
              className="item"
              onClick={
                isDevToolDialogOpen ? closeDevToolDialog : openDevToolDialog
              }
            >
              <DeveloperModeIcon />
            </IconButton>
          </Tooltip>

          <Tooltip title="Node list" placement="right" enterDelay={1000}>
            <IconButton
              className="nodes item"
              onClick={() => setIsNodeDialogOpen(true)}
            >
              <StorageIcon />
            </IconButton>
          </Tooltip>
        </div>
      </header>

      <ListWorkspacesDialog />
    </StyledAppBar>
  );
};

export default AppBar;
