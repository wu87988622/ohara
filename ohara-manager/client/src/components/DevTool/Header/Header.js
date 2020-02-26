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
import { get, isEmpty } from 'lodash';
import CloseIcon from '@material-ui/icons/Close';
import IconButton from '@material-ui/core/IconButton';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import Grid from '@material-ui/core/Grid';

import { KIND, CELL_PROPS } from 'const';
import * as context from 'context';
import { Tooltip } from 'components/common/Tooltip';
import { TAB } from 'context/devTool/const';
import { usePrevious } from 'utils/hooks';
import { ControllerLog, ControllerTopic } from './Controller';
import { StyledHeader } from './HeaderStyles';

const Header = () => {
  const { tabName, setTabName } = context.useDevTool();
  const { isOpen, close: closeDialog } = context.useDevToolDialog();

  const { setSelectedCell } = context.usePipelineActions();
  const logActions = context.useLogActions();
  const topicDataActions = context.useTopicDataActions();

  const { selectedCell } = context.usePipelineState();
  const { isFetching: isFetchingLog } = context.useLogState();
  const { isFetching: isFetchingTopic } = context.useTopicDataState();

  const prevSelectedCell = usePrevious(selectedCell);
  const prevTab = usePrevious(tabName);

  React.useEffect(() => {
    if (!isOpen || (prevSelectedCell === selectedCell && prevTab === tabName))
      return;

    const getService = kind => {
      if (kind === KIND.source || kind === KIND.sink) return KIND.worker;
      if (kind === KIND.topic) return KIND.broker;
      if (kind === KIND.stream) return KIND.stream;
      return '';
    };

    const kind = get(selectedCell, CELL_PROPS.kind, '');

    if (kind === KIND.topic) {
      if (tabName === TAB.log) {
        logActions.setLogType(KIND.broker);
      } else {
        topicDataActions.setName(get(selectedCell, CELL_PROPS.displayName, ''));
      }
    } else if (!isEmpty(kind)) {
      // the selected cell is a source, sink, or stream
      setTabName(TAB.log);
      const service = getService(kind);
      logActions.setLogType(service);
    }
  }, [
    isOpen,
    prevSelectedCell,
    selectedCell,
    logActions,
    topicDataActions,
    prevTab,
    tabName,
    setTabName,
  ]);

  const handleTabChange = (event, currentTab) => {
    // handle the tab change directly
    // we need to clear the select cell to avoid the following situation
    // click log tab -> click source -> click topics tab will be disabled
    setSelectedCell(null);
    setTabName(currentTab);
  };

  return (
    <StyledHeader
      container
      direction="row"
      justify="space-between"
      alignItems="center"
    >
      <Grid item xs={4} lg={4}>
        <Tabs
          value={tabName}
          indicatorColor="primary"
          textColor="primary"
          onChange={handleTabChange}
        >
          <Tab value={TAB.topic} label={TAB.topic} disabled={isFetchingTopic} />
          <Tab value={TAB.log} label={TAB.log} disabled={isFetchingLog} />
        </Tabs>
      </Grid>

      <Grid item xs={8} lg={7}>
        <div className="items">
          {tabName === TAB.topic && <ControllerTopic />}
          {tabName === TAB.log && <ControllerLog />}
          <Tooltip title="Close this panel">
            <IconButton className="item" onClick={closeDialog} size="small">
              <CloseIcon />
            </IconButton>
          </Tooltip>
        </div>
      </Grid>
    </StyledHeader>
  );
};

export default Header;
