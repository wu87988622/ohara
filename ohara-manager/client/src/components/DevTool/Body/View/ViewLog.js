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
import PropTypes from 'prop-types';

import * as context from 'context';
import { VirtualizedList } from 'components/common/List';
import { useCurrentLogs } from 'components/DevTool/hooks';
import { StyledLogDiv, StyledContent } from './ViewStyles';

const ViewLog = () => {
  const { isFetching } = context.useLogState();
  const currentLog = useCurrentLogs();

  const rowRenderer = ({ rowData: log, style }) => {
    return <StyledContent style={style}>{log}</StyledContent>;
  };
  rowRenderer.propTypes = {
    rowData: PropTypes.object.isRequired,
    style: PropTypes.object.isRequired,
  };

  return (
    <StyledLogDiv data-testid="view-log-list">
      <VirtualizedList
        data={currentLog}
        rowRenderer={rowRenderer}
        isLoading={isFetching}
      />
    </StyledLogDiv>
  );
};

export { ViewLog };
