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
import { get } from 'lodash';
import Grid from '@material-ui/core/Grid';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import Divider from '@material-ui/core/Divider';
import Button from '@material-ui/core/Button';

import { FullScreenDialog } from 'components/common/Dialog';
import { AddTopicDialog } from 'components/Topic';
import Badge from 'components/common/Badge';
import {
  useEditWorkspaceDialog,
  useAddTopicDialog,
  useEditWorkspace,
  useWorkspace,
} from 'context';

import { Tabs as WorkspaceTabs } from '.';
import { FileUpload } from './File';
import MoreActions from './MoreActions';
import RestartIndicator from './RestartIndicator';
import { TabPanel, SettingsTab, FilesTab, TopicsTab } from './Tabs';
import { Wrapper } from './EditWorkspaceStyles';

const EditWorkspace = () => {
  const {
    isOpen: isEditWorkspaceDialogOpen,
    close: closeEditWorkspaceDialog,
    data: editWorkspaceDialogData,
    setData: setEditWorkspaceDialogData,
  } = useEditWorkspaceDialog();
  const { open: openAddTopicDialog } = useAddTopicDialog();
  const { currentWorkspace } = useWorkspace();
  const { dirties } = useEditWorkspace();

  const handleChange = (event, newTab) => {
    setEditWorkspaceDialogData({ tab: newTab });
  };

  const tab = get(editWorkspaceDialogData, 'tab', 'overview');
  return (
    <>
      <FullScreenDialog
        title={`Workspace ${currentWorkspace.name}`}
        open={isEditWorkspaceDialogOpen}
        handleClose={closeEditWorkspaceDialog}
      >
        <Wrapper>
          <Grid
            container
            justify="flex-end"
            alignItems="center"
            className="actions"
          >
            <Button
              variant="contained"
              color="primary"
              onClick={openAddTopicDialog}
              className="button"
            >
              ADD TOPIC
            </Button>
            <FileUpload
              handleTabChange={handleChange}
              shouldRedirect={tab !== WorkspaceTabs.FILES}
            >
              <Button variant="outlined" color="primary" className="button">
                UPLOAD FILE
              </Button>
            </FileUpload>
            <MoreActions />
          </Grid>
          <RestartIndicator invisible={dirties.all === 0} />
          <Tabs
            value={tab}
            onChange={handleChange}
            indicatorColor="primary"
            textColor="primary"
          >
            {/* Feature is disabled because it's not implemented in 0.9 */
            false && (
              <Tab
                label={WorkspaceTabs.OVERVIEW}
                value={WorkspaceTabs.OVERVIEW}
              />
            )}

            <Tab label={WorkspaceTabs.TOPICS} value={WorkspaceTabs.TOPICS} />
            <Tab label={WorkspaceTabs.FILES} value={WorkspaceTabs.FILES} />

            {/* Feature is disabled because it's not implemented in 0.9 */
            false && (
              <Tab
                label={
                  <Badge
                    badgeContent={dirties.all}
                    invisible={dirties.all === 0}
                    color="warning"
                  >
                    {WorkspaceTabs.SETTINGS}
                  </Badge>
                }
                value={WorkspaceTabs.SETTINGS}
              />
            )}
          </Tabs>

          <Divider />

          {/* Feature is disabled because it's not implemented in 0.9 */
          false && (
            <TabPanel
              value={tab}
              index={WorkspaceTabs.OVERVIEW}
              className="tab-panel"
            >
              Overview
            </TabPanel>
          )}
          <TabPanel
            value={tab}
            index={WorkspaceTabs.TOPICS}
            className="tab-panel"
          >
            <TopicsTab />
          </TabPanel>
          <TabPanel
            value={tab}
            index={WorkspaceTabs.FILES}
            className="tab-panel"
          >
            <FilesTab />
          </TabPanel>

          {/* Feature is disabled because it's not implemented in 0.9 */
          false && (
            <TabPanel
              value={tab}
              index={WorkspaceTabs.SETTINGS}
              className="tab-panel"
            >
              <SettingsTab />
            </TabPanel>
          )}
        </Wrapper>
      </FullScreenDialog>
      <AddTopicDialog />
    </>
  );
};

export default EditWorkspace;
