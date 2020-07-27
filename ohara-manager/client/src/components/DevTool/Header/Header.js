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

import { KIND, CELL_PROP, DialogToggleType } from 'const';
import * as hooks from 'hooks';
import { Tooltip } from 'components/common/Tooltip';
import { DevToolTabName } from 'const';
import { usePrevious } from 'utils/hooks';
import { ControllerLog, ControllerTopic } from './Controller';
import { StyledHeader } from './HeaderStyles';
import { isShabondi } from 'components/Pipeline/PipelineUtils';

const Header = () => {
  const devToolDialog = hooks.useDevToolDialog();
  const tabName = devToolDialog?.data?.tabName;

  const setSelectedCell = hooks.useSetSelectedCellAction();
  const setLogQueryParams = hooks.useSetDevToolLogQueryParams();
  const setTopicQueryParams = hooks.useSetDevToolTopicQueryParams();

  const selectedCell = hooks.useCurrentPipelineCell();
  const { isFetching: isFetchingLog } = hooks.useDevToolLog();
  const { isFetching: isFetchingTopic } = hooks.useDevToolTopicData();

  const prevSelectedCell = usePrevious(selectedCell);
  const prevTab = usePrevious(tabName);

  React.useEffect(() => {
    if (
      !devToolDialog.isOpen ||
      (prevSelectedCell === selectedCell && prevTab === tabName)
    )
      return;

    const getService = (kind, className) => {
      // Shabondis are included in the source and sink but with a different mechanism
      // just like stream, so we need to handle it differently
      if (isShabondi(className)) return KIND.shabondi;

      if (kind === KIND.source || kind === KIND.sink) return KIND.worker;
      if (kind === KIND.topic) return KIND.broker;
      if (kind === KIND.stream) return KIND.stream;
      return '';
    };

    const kind = get(selectedCell, CELL_PROP.kind, '');
    const className = get(selectedCell, CELL_PROP.className, '');

    if (kind === KIND.topic) {
      if (tabName === DevToolTabName.LOG) {
        setLogQueryParams({ logType: KIND.broker });
      } else {
        setTopicQueryParams({
          name: get(selectedCell, CELL_PROP.name, ''),
        });
      }
    } else if (!isEmpty(kind)) {
      // the selected cell is a source, sink, or stream
      devToolDialog.open({ tabName: DevToolTabName.LOG });
      const service = getService(kind, className);
      setLogQueryParams({ logType: service });
      if (isShabondi(className)) {
        setLogQueryParams({
          shabondiName: get(selectedCell, CELL_PROP.displayName, ''),
        });
      } else if (kind === KIND.stream) {
        setLogQueryParams({
          streamName: get(selectedCell, CELL_PROP.displayName, ''),
        });
      }
    }
  }, [
    devToolDialog,
    prevSelectedCell,
    selectedCell,
    prevTab,
    setTopicQueryParams,
    setLogQueryParams,
    tabName,
  ]);

  const handleTabChange = (event, currentTab) => {
    // handle the tab change directly
    // we need to clear the select cell to avoid the following situation
    // click log tab -> click source -> click topics tab will be disabled
    setSelectedCell(null);
    devToolDialog.open({ tabName: currentTab });
  };

  return (
    <StyledHeader
      alignItems="center"
      container
      direction="row"
      justify="space-between"
    >
      <Grid item lg={4} xs={4}>
        <Tabs
          indicatorColor="primary"
          onChange={handleTabChange}
          textColor="primary"
          value={tabName}
        >
          <Tab
            disabled={isFetchingTopic}
            label={DevToolTabName.TOPIC}
            value={DevToolTabName.TOPIC}
          />
          <Tab
            disabled={isFetchingLog}
            label={DevToolTabName.LOG}
            value={DevToolTabName.LOG}
          />
        </Tabs>
      </Grid>

      <Grid item lg={7} xs={8}>
        <div className="items">
          {tabName === DevToolTabName.TOPIC && <ControllerTopic />}
          {tabName === DevToolTabName.LOG && <ControllerLog />}
          <Tooltip title="Close this panel">
            <IconButton
              className="item"
              onClick={() => devToolDialog.toggle(DialogToggleType.FORCE_CLOSE)}
              size="small"
            >
              <CloseIcon />
            </IconButton>
          </Tooltip>
        </div>
      </Grid>
    </StyledHeader>
  );
};

export default Header;
