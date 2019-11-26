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
import Grid from '@material-ui/core/Grid';

import { useEditWorkspaceDialog } from 'context';
import { SubTabs } from 'components/Workspace/Edit';
import { SettingsMenu } from 'components/Workspace/Edit/Settings';
import TabPanel from './TabPanel';

function SettingsTab() {
  const { data = {} } = useEditWorkspaceDialog();
  const { subTab = SubTabs.NONE } = data;

  return (
    <>
      <Grid container spacing={3}>
        <Grid item xs={3}>
          <SettingsMenu />
        </Grid>
        <Grid item xs={9}>
          <TabPanel index={subTab} value={SubTabs.SETTINGS}>
            Settings
          </TabPanel>
          <TabPanel index={subTab} value={SubTabs.PLUGINS}>
            Plugins
          </TabPanel>
          <TabPanel index={subTab} value={SubTabs.NODES}>
            Nodes
          </TabPanel>
        </Grid>
      </Grid>
    </>
  );
}

export default SettingsTab;
