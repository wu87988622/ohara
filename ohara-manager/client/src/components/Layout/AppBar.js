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
import { get, isEmpty } from 'lodash';
import clx from 'classnames';
import AppsIcon from '@material-ui/icons/Apps';
import DeveloperModeIcon from '@material-ui/icons/DeveloperMode';
import AssignmentIcon from '@material-ui/icons/Assignment';
import StorageIcon from '@material-ui/icons/Storage';
import AddIcon from '@material-ui/icons/Add';
import IconButton from '@material-ui/core/IconButton';
import Badge from '@material-ui/core/Badge';
import Link from '@material-ui/core/Link';

import * as context from 'context';
import * as hooks from 'hooks';
import { ReactComponent as Logo } from 'images/logo.svg';
import { StyledAppBar } from './AppBarStyles';
import { Tooltip } from 'components/common/Tooltip';
import NodeListDialog from 'components/Node/NodeListDialog';

// Since Mui doesn't provide a vertical AppBar, we're creating our own
// therefore, this AppBar has nothing to do with Muis
const AppBar = () => {
  const allNodes = hooks.useAllNodes();
  const workspaces = hooks.useAllWorkspaces();
  const workspace = hooks.useWorkspace();
  const openIntro = hooks.useOpenIntroAction();
  const {
    toggle: toggleDevTool,
    close: closeDevTool,
  } = context.useDevToolDialog();
  const {
    toggle: toggleEventLog,
    close: closeEventLog,
  } = context.useEventLogDialog();
  const { data: configuratorInfo } = context.useConfiguratorState();
  const { toggle: toggleWorkspaceList } = context.useListWorkspacesDialog();
  const { data: notifications } = hooks.useEventNotifications();
  const createWorkspaceState = hooks.useCreateWorkspaceState();
  const switchWorkspace = hooks.useSwitchWorkspaceAction();
  const [isNodeListDialogOpen, setIsNodeListDialogOpen] = useState(false);

  return (
    <StyledAppBar>
      <header>
        <div className="brand">
          <Logo width="38" height="38" />
        </div>
        <div className="workspace-list">
          {workspaces.map(({ name }) => {
            const displayName = name.substring(0, 2).toUpperCase();

            return (
              <Tooltip key={name} title={name} placement="right">
                <Link
                  className={clx('workspace-name', 'item', {
                    'active-link': name === workspace?.name,
                  })}
                  onClick={() => {
                    if (name !== workspace?.name) switchWorkspace(name);
                  }}
                >
                  {displayName}
                </Link>
              </Tooltip>
            );
          })}

          <Tooltip title="Create a new workspace" placement="right">
            <div className="add-workspace item" onClick={openIntro}>
              <AddIcon />
            </div>
          </Tooltip>
        </div>

        <div className="tools">
          <Tooltip title="Workspace list" placement="right">
            <IconButton
              className="workspace-list item"
              onClick={() =>
                isEmpty(workspaces) ? openIntro() : toggleWorkspaceList()
              }
              disabled={createWorkspaceState?.loading}
            >
              <AppsIcon />
            </IconButton>
          </Tooltip>

          <Tooltip title="Event logs" placement="right">
            <IconButton
              className="event-logs item"
              onClick={() => {
                toggleEventLog();
                closeDevTool();
              }}
            >
              <Badge
                badgeContent={get(notifications, 'error', 0)}
                color="secondary"
              >
                <AssignmentIcon />
              </Badge>
            </IconButton>
          </Tooltip>

          <Tooltip title="Developer Tools" placement="right">
            <IconButton
              className="developer-tools item"
              onClick={() => {
                toggleDevTool();
                closeEventLog();
              }}
            >
              <DeveloperModeIcon />
            </IconButton>
          </Tooltip>

          <Tooltip title="Node list" placement="right">
            <IconButton
              className="node-list item"
              onClick={() => setIsNodeListDialogOpen(true)}
            >
              <StorageIcon />
            </IconButton>
          </Tooltip>
        </div>
      </header>
      <NodeListDialog
        dialogTitle="All nodes"
        isOpen={isNodeListDialogOpen}
        mode={configuratorInfo?.mode}
        nodes={allNodes}
        onClose={() => setIsNodeListDialogOpen(false)}
        tableTitle="All nodes"
      />
    </StyledAppBar>
  );
};

export default AppBar;
