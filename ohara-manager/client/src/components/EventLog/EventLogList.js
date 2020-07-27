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
import PropTypes from 'prop-types';
import { isEqual } from 'lodash';

import { VirtualizedList } from 'components/common/List';
import EventLogContentDialog from './EventLogContentDialog';
import EventLogRow from './EventLogRow';

const MemorizeList = React.memo(
  ({ data, isLoading, onRowClick }) => (
    <VirtualizedList
      autoScrollToBottom
      data={data}
      isLoading={isLoading}
      onRowClick={onRowClick}
      rowRenderer={EventLogRow}
    />
  ),
  (prevProps, nextProps) =>
    isEqual(prevProps.data, nextProps.data) &&
    // Even if data is not changed
    // We still need to re-render for "isLoading" changed
    prevProps.isLoading === nextProps.isLoading,
);

MemorizeList.propTypes = {
  data: PropTypes.array.isRequired,
  isLoading: PropTypes.bool.isRequired,
  onRowClick: PropTypes.func.isRequired,
};

const EventLogList = ({ data, isFetching }) => {
  const [isContentOpen, setIsContentOpen] = useState(false);
  const [activeRowData, setActiveRowData] = useState(null);

  return (
    <>
      <MemorizeList
        data={data}
        isLoading={isFetching}
        onRowClick={(rowData) => {
          setIsContentOpen(true);
          setActiveRowData(rowData);
        }}
      />
      <EventLogContentDialog
        data={activeRowData}
        isOpen={isContentOpen}
        onClose={() => setIsContentOpen(false)}
      />
    </>
  );
};

EventLogList.propTypes = {
  data: PropTypes.array.isRequired,
  isFetching: PropTypes.bool,
};

EventLogList.defaultProps = {
  isFetching: false,
};

export default EventLogList;
