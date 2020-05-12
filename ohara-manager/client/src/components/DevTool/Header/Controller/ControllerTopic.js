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
import { isEmpty } from 'lodash';
import IconButton from '@material-ui/core/IconButton';
import RefreshIcon from '@material-ui/icons/Refresh';
import SearchIcon from '@material-ui/icons/Search';
import OpenInNewIcon from '@material-ui/icons/OpenInNew';
import TextField from '@material-ui/core/TextField';

import * as hooks from 'hooks';
import { Tooltip } from 'components/common/Tooltip';
import { Button } from 'components/common/Form';
import Popover from 'components/common/Popover';
import Select from 'components/common/Select';
import { TAB } from 'context/devTool/const';
import { usePrevious } from 'utils/hooks';
import { StyledSearchBody } from './ControllerStyles';

const ControllerTopic = () => {
  const topics = hooks.useTopicsInWorkspace();

  const setTopicQueryParams = hooks.useSetDevToolTopicQueryParams();
  const refetchTopic = hooks.useRefetchDevToolTopicDataAction();
  const { query, isFetching } = hooks.useDevToolTopicData();
  const { name, limit } = query;

  const prevName = usePrevious(name);

  const handleOpenNewWindow = () => {
    if (name)
      window.open(
        `${window.location}/view?type=${TAB.topic}&topicName=${name}&topicLimit=${limit}`,
      );
  };

  const getDisableState = () => {
    // true -> disabled
    if (isFetching) return true;
    if (isEmpty(name)) return true;
  };

  return (
    <>
      <Tooltip title="Select topic">
        <Select
          value={topics.find(topic => topic.name === name)?.displayName || ''}
          onChange={event =>
            prevName !== event.target.value &&
            setTopicQueryParams({
              name: topics.find(
                topic => topic.displayName === event.target.value,
              ).name,
            })
          }
          list={topics.map(topic => topic.displayName)}
          disabled={isFetching}
        />
      </Tooltip>
      <Tooltip title="Fetch the data again">
        <IconButton
          className="item"
          disabled={getDisableState()}
          onClick={refetchTopic}
          size="small"
        >
          <RefreshIcon />
        </IconButton>
      </Tooltip>
      <Popover
        showTooltip
        tooltipTitle="Query with different parameters"
        trigger={
          <IconButton
            disabled={getDisableState()}
            className="item"
            size="small"
          >
            <SearchIcon />
          </IconButton>
        }
      >
        <StyledSearchBody tab={TAB.topic}>
          <label>Rows per query</label>
          <TextField
            type="number"
            defaultValue={limit}
            onChange={event =>
              setTopicQueryParams({ limit: Number(event.target.value) })
            }
            disabled={isEmpty(name)}
          />
          <Button
            variant="contained"
            onClick={refetchTopic}
            disabled={isEmpty(name)}
          >
            QUERY
          </Button>
        </StyledSearchBody>
      </Popover>
      <Tooltip title="Open in a new window">
        <IconButton
          className="item"
          onClick={handleOpenNewWindow}
          size="small"
          disabled={getDisableState()}
        >
          <OpenInNewIcon />
        </IconButton>
      </Tooltip>
    </>
  );
};

export { ControllerTopic };
