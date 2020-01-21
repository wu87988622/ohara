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
import SplitPane from 'react-split-pane';

import AppBar from './AppBar';
import Pipeline from 'components/Pipeline';
import Navigator from './Navigator';
import EventLog from 'components/EventLog';
import * as context from 'context';
import { DevToolDialog } from 'components/DevTool';
import { Wrapper } from './AppLayoutStyles';

const AppLayout = () => {
  const { isOpen: isDevToolOpen } = context.useDevToolDialog();
  const { isOpen: isEventLogOpen } = context.useEventLogDialog();

  return (
    <Wrapper>
      <SplitPane split="vertical" defaultSize={64} allowResize={false}>
        <AppBar className="app-bar" />
        <SplitPane
          split="horizontal"
          primary="second"
          defaultSize={isDevToolOpen || isEventLogOpen ? 240 : 0}
          minSize={isDevToolOpen || isEventLogOpen ? 80 : 0}
          maxSize={isDevToolOpen || isEventLogOpen ? 480 : 0}
        >
          <SplitPane
            split="vertical"
            defaultSize={224}
            minSize={224}
            maxSize={480}
          >
            <Navigator />
            <Pipeline />
          </SplitPane>
          {isEventLogOpen && <EventLog />}
          {isDevToolOpen && <DevToolDialog />}
          <div />
        </SplitPane>
      </SplitPane>
    </Wrapper>
  );
};

export default AppLayout;
