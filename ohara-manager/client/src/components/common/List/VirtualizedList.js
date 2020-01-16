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

import React, { useRef, useEffect } from 'react';
import PropTypes from 'prop-types';
import { isEmpty } from 'lodash';

import { CellMeasurerCache } from 'react-virtualized/dist/commonjs/CellMeasurer';
import { List } from 'react-virtualized/dist/commonjs/List';
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';
import { CellMeasurer } from 'react-virtualized/dist/commonjs/CellMeasurer';
import { TableLoader } from 'components/common/Loader';

const VirtualizedList = props => {
  const { isLoading, data, rowRenderer, autoScrollToBottom } = props;
  const listRef = useRef(null);
  const cache = new CellMeasurerCache({
    defaultHeight: 20,
    fixedWidth: true,
  });

  useEffect(() => {
    if (listRef.current && autoScrollToBottom && !isEmpty(data)) {
      setTimeout(() => {
        listRef.current.scrollToRow(data.length);
      }, 0);
    }
  }, [autoScrollToBottom, data]);

  const RowRendererWrapper = ({
    index,
    isScrolling,
    isVisible,
    key,
    parent,
    style,
  }) => (
    <CellMeasurer
      cache={cache}
      columnIndex={0}
      key={key}
      parent={parent}
      rowIndex={index}
    >
      {rowRenderer({
        index,
        isScrolling,
        isVisible,
        key,
        parent,
        rowData: data[index],
        style,
      })}
    </CellMeasurer>
  );

  RowRendererWrapper.propTypes = {
    index: PropTypes.number,
    isScrolling: PropTypes.bool,
    isVisible: PropTypes.bool,
    key: PropTypes.string,
    parent: PropTypes.any,
    style: PropTypes.object,
  };

  if (isLoading) return <TableLoader />;

  return (
    <AutoSizer onResize={() => cache.clearAll()}>
      {({ width, height }) => (
        <List
          ref={listRef}
          deferredMeasurementCache={cache}
          height={height}
          rowCount={data.length}
          rowHeight={cache.rowHeight}
          rowRenderer={RowRendererWrapper}
          overscanRowCount={0}
          width={width}
        />
      )}
    </AutoSizer>
  );
};

VirtualizedList.propTypes = {
  autoScrollToBottom: PropTypes.bool,
  data: PropTypes.array.isRequired,
  isLoading: PropTypes.bool,
  rowRenderer: PropTypes.func.isRequired,
};

VirtualizedList.defaultProps = {
  autoScrollBottom: false,
  data: [],
  isLoading: false,
};

export default VirtualizedList;
