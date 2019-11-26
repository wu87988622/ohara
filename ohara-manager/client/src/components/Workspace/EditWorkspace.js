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
import IconButton from '@material-ui/core/IconButton';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import MoreIcon from '@material-ui/icons/MoreVert';
import Divider from '@material-ui/core/Divider';
import Button from '@material-ui/core/Button';

import { FullScreenDialog } from 'components/common/Dialog';
import { AddTopicDialog } from 'components/Topic';
import { useEditWorkspaceDialog, useAddTopicDialog } from 'context';

import { Tabs as WorkspaceTabs } from '.';
import TabPanel from './TabPanel';
import { TopicsTab } from './TopicsTab';
import { FilesTab, FileUpload } from './File';
import { SettingsTab } from './Settings';
import { Wrapper } from './EditWorkspaceStyles';

const EditWorkspace = () => {
  const {
    isOpen: isEditWorkspaceDialogOpen,
    close: closeEditWorkspaceDialog,
    data: editWorkspaceDialogData,
    setData: setEditWorkspaceDialogData,
  } = useEditWorkspaceDialog();
  const { open: openAddTopicDialog } = useAddTopicDialog();

  const handleChange = (event, newTab) => {
    setEditWorkspaceDialogData({ tab: newTab });
  };

  const tab = get(editWorkspaceDialogData, 'tab', 'overview');

  return (
    <>
      <FullScreenDialog
        title="Your workspace for oharadevteam"
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
            <FileUpload>
              <Button variant="outlined" color="primary" className="button">
                UPLOAD FILE
              </Button>
            </FileUpload>

            <IconButton
              aria-label="display more actions"
              edge="end"
              color="inherit"
            >
              <MoreIcon />
            </IconButton>
          </Grid>
          <Tabs
            value={tab}
            onChange={handleChange}
            indicatorColor="primary"
            textColor="primary"
          >
            <Tab
              label={WorkspaceTabs.OVERVIEW}
              value={WorkspaceTabs.OVERVIEW}
            />
            <Tab label={WorkspaceTabs.TOPICS} value={WorkspaceTabs.TOPICS} />
            <Tab label={WorkspaceTabs.FILES} value={WorkspaceTabs.FILES} />
            <Tab
              label={WorkspaceTabs.SETTINGS}
              value={WorkspaceTabs.SETTINGS}
            />
          </Tabs>
          <Divider />
          <TabPanel
            value={tab}
            index={WorkspaceTabs.OVERVIEW}
            className="tab-panel"
          >
            Overview
          </TabPanel>
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
          <TabPanel
            value={tab}
            index={WorkspaceTabs.SETTINGS}
            className="tab-panel"
          >
            <SettingsTab />
          </TabPanel>
        </Wrapper>
      </FullScreenDialog>
      <AddTopicDialog />
    </>
  );
};

export default EditWorkspace;
