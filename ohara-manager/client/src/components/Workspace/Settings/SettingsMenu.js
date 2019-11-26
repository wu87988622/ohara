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
import Paper from '@material-ui/core/Paper';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';

import { useEditWorkspaceDialog } from 'context';

import { SubTabs, Segments } from '.';
import { Wrapper } from './SettingsMenuStyles';

function SettingsMenu() {
  const { data, setData } = useEditWorkspaceDialog();
  const { subTab = SubTabs.NONE, segment = Segments.NONE } = data;

  const handleClick = (clickedTab, clickedSegment) => () => {
    if (clickedTab === SubTabs.SETTINGS && clickedSegment === Segments.NONE) {
      if (segment === Segments.NONE) {
        const defaultSegment = Segments.WORKER;
        setData({ ...data, subTab: clickedTab, segment: defaultSegment });
      }
      return;
    }
    setData({ ...data, subTab: clickedTab, segment: clickedSegment });
  };

  return (
    <Wrapper>
      <Paper>
        <List component="nav">
          <ListItem
            button
            selected={subTab === SubTabs.SETTINGS}
            onClick={handleClick(SubTabs.SETTINGS, Segments.NONE)}
          >
            <ListItemText primary="Settings" />
          </ListItem>
          <List component="div" disablePadding>
            <ListItem
              button
              className="nested"
              selected={
                subTab === SubTabs.SETTINGS && segment === Segments.WORKER
              }
              onClick={handleClick(SubTabs.SETTINGS, Segments.WORKER)}
            >
              <ListItemText primary="Worker" />
            </ListItem>
            <ListItem
              button
              className="nested"
              selected={
                subTab === SubTabs.SETTINGS && segment === Segments.BROKER
              }
              onClick={handleClick(SubTabs.SETTINGS, Segments.BROKER)}
            >
              <ListItemText primary="Broker" />
            </ListItem>
            <ListItem
              button
              className="nested"
              selected={
                subTab === SubTabs.SETTINGS && segment === Segments.ZOOKEEPER
              }
              onClick={handleClick(SubTabs.SETTINGS, Segments.ZOOKEEPER)}
            >
              <ListItemText primary="Zookeeper" />
            </ListItem>
          </List>
          <ListItem
            button
            selected={subTab === SubTabs.PLUGINS}
            onClick={handleClick(SubTabs.PLUGINS, Segments.NONE)}
          >
            <ListItemText primary="Plugins" />
          </ListItem>
          <ListItem
            button
            selected={subTab === SubTabs.NODES}
            onClick={handleClick(SubTabs.NODES, Segments.NONE)}
          >
            <ListItemText primary="Nodes" />
          </ListItem>
        </List>
      </Paper>
    </Wrapper>
  );
}

export default SettingsMenu;
