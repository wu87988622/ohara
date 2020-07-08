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
import WarningIcon from '@material-ui/icons/Warning';

import * as context from 'context';
import * as hooks from 'hooks';
import { ReactComponent as Logo } from 'images/logo.svg';
import { StyledAppBar } from './AppBarStyles';
import { Tooltip } from 'components/common/Tooltip';
import { IconWrapper } from 'components/common/Icon';
import NodeListDialog from './NodeListDialog';
import { isStable as isStableWorkspace } from 'utils/workspace';

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
    isOpen: isEventLogOpen,
  } = context.useEventLogDialog();
  const { data: configuratorInfo } = context.useConfiguratorState();
  const { toggle: toggleWorkspaceList } = context.useListWorkspacesDialog();
  const { data: notifications } = hooks.useEventNotifications();
  const switchWorkspace = hooks.useSwitchWorkspaceAction();
  const initEventLogs = hooks.useInitEventLogsAction();
  const [isNodeListDialogOpen, setIsNodeListDialogOpen] = useState(false);
  const showMessage = hooks.useShowMessage();

  return (
    <StyledAppBar id="app-bar">
      <header>
        <div className="brand">
          <Logo height="38" width="38" />
        </div>
        <div className="workspace-list">
          {workspaces.map((wk) => {
            const { name } = wk;
            const displayName = name.substring(0, 2).toUpperCase();
            const isStable = isStableWorkspace(wk);

            return (
              <Tooltip key={name} placement="right" title={name}>
                <Badge
                  badgeContent={
                    <IconWrapper severity="warning">
                      <WarningIcon fontSize="small" />
                    </IconWrapper>
                  }
                  invisible={isStable}
                  title="Unstable workspace"
                >
                  <Link
                    className={clx('workspace-name', 'item', {
                      'active-link': name === workspace?.name,
                    })}
                    onClick={() => {
                      if (!isStable) {
                        showMessage(`This is an unstable workspace: ${name}`);
                        return;
                      }
                      if (name !== workspace?.name) switchWorkspace(name);
                    }}
                  >
                    {displayName}
                  </Link>
                </Badge>
              </Tooltip>
            );
          })}

          <Tooltip placement="right" title="Create a new workspace">
            <div className="add-workspace item" onClick={openIntro}>
              <AddIcon />
            </div>
          </Tooltip>
        </div>

        <div className="tools">
          <Tooltip placement="right" title="Workspace list">
            <IconButton
              className="workspace-list item"
              data-testid="workspace-list-button"
              onClick={() =>
                isEmpty(workspaces) ? openIntro() : toggleWorkspaceList()
              }
            >
              <AppsIcon />
            </IconButton>
          </Tooltip>

          <Tooltip placement="right" title="Event logs">
            <IconButton
              className="event-logs item"
              onClick={() => {
                toggleEventLog();
                closeDevTool();
                // during clicking action, the event log dialog is still "opened"
                // we should assert isOpen=true(which will close dialog later) and trigger the initEventLogs
                if (isEventLogOpen) initEventLogs();
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

          <Tooltip placement="right" title="Developer Tools">
            <IconButton
              className="developer-tools item"
              onClick={() => {
                toggleDevTool();
                closeEventLog();
                // We should initial event logs when clicking the devTool button
                // since it will trigger close event log whether devTool opens or not
                initEventLogs();
              }}
            >
              <DeveloperModeIcon />
            </IconButton>
          </Tooltip>

          <Tooltip placement="right" title="Node list">
            <IconButton
              className="node-list item"
              data-testid="nodes-dialog-open-button"
              onClick={() => setIsNodeListDialogOpen(true)}
            >
              <StorageIcon />
            </IconButton>
          </Tooltip>
        </div>
      </header>
      <NodeListDialog
        isOpen={isNodeListDialogOpen}
        mode={configuratorInfo?.mode}
        nodes={allNodes}
        onClose={() => setIsNodeListDialogOpen(false)}
      />
    </StyledAppBar>
  );
};

export default AppBar;
