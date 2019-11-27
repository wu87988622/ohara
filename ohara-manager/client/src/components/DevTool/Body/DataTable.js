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

import { isEmpty, capitalize } from 'lodash';
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import VisibilityIcon from '@material-ui/icons/Visibility';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';

import { List } from 'react-virtualized/dist/commonjs/List';
import { AutoSizer } from 'react-virtualized/dist/commonjs/AutoSizer';
import { CellMeasurer } from 'react-virtualized/dist/commonjs/CellMeasurer';

import { TableLoader } from 'components/common/Loader';
import { Table } from 'components/common/Table';
import { Dialog } from 'components/common/Dialog';
import { StyledTopicView } from './BodyStyles';

import { tabName } from '../DevToolDialog';

const ErrorCell = styled(TableCell)(
  ({ theme }) => css`
    background-color: ${theme.palette.common.white};

    /* we only change the text color of "the first cell" right after the "view icon" element */
    + .error-message {
      color: ${theme.palette.text.disabled};
    }
  `,
);

const DataRow = styled(TableRow)`
  height: 50px;
`;

const DataCell = styled(TableCell)`
  max-width: 50px;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
`;

const Content = styled.div`
  padding-bottom: ${props => props.theme.spacing(1)}px;
`;

const getHeaders = topicData => {
  let headers = [];
  topicData.forEach(message => {
    if (message.value) {
      const keys = Object.keys(message.value);
      keys.forEach(key => {
        if (!headers.includes(key)) headers.push(key);
      });
    }
  });
  // the detail view header must insert to the first element
  // here we append the "empty header cell" if data is not empty
  if (headers.length > 0) headers.sort().unshift('');
  return headers;
};

const DataTable = props => {
  const [viewTopicMessage, setViewTopicMessage] = useState({});

  const { data, type, ...others } = props;
  const { cache, windowOpts } = others;

  const renderDataBody = topicData => {
    const headers = getHeaders(topicData);
    // we skip the detail view header
    headers.shift();
    return data.topicData.map((message, rowIdx) => (
      <DataRow key={rowIdx}>
        {message.value ? (
          <>
            <DataCell key={rowIdx + '_view'}>
              <VisibilityIcon
                onClick={() => {
                  setViewTopicMessage(message);
                }}
              />
            </DataCell>
            {headers.map((header, headerIdx) =>
              message.value[header] ? (
                <DataCell key={rowIdx + '_' + headerIdx}>
                  {message.value[header]}
                </DataCell>
              ) : (
                <DataCell key={rowIdx + '_' + headerIdx} />
              ),
            )}
          </>
        ) : (
          <>
            <ErrorCell key={rowIdx + '_view'}>
              <VisibilityIcon
                onClick={() => {
                  setViewTopicMessage(message);
                }}
              />
            </ErrorCell>
            <ErrorCell
              className="error-message"
              colSpan={headers.length}
              key={rowIdx + '_error'}
            >
              {message.error}
            </ErrorCell>
          </>
        )}
      </DataRow>
    ));
  };

  const rowRenderer = ({ index, key, parent, style }) => {
    return (
      <CellMeasurer
        cache={cache}
        columnIndex={0}
        key={key}
        parent={parent}
        rowIndex={index}
      >
        <Content style={style}>{data.hostLog[index]}</Content>
      </CellMeasurer>
    );
  };
  rowRenderer.propTypes = {
    index: PropTypes.number.isRequired,
    key: PropTypes.string.isRequired,
    parent: PropTypes.any.isRequired,
    style: PropTypes.object.isRequired,
  };

  switch (type) {
    case tabName.topic:
      return (
        <>
          <Table
            fixedHeader
            headers={getHeaders(data.topicData).map(header =>
              capitalize(header),
            )}
            isLoading={data.isLoading}
            children={renderDataBody(data.topicData)}
          />
          <Dialog
            title="View topic source"
            open={!isEmpty(viewTopicMessage)}
            handleClose={() => setViewTopicMessage({})}
            children={
              <StyledTopicView>
                <textarea
                  readOnly
                  value={JSON.stringify(
                    viewTopicMessage.value
                      ? viewTopicMessage.value
                      : viewTopicMessage.error,
                    null,
                    2,
                  )}
                />
              </StyledTopicView>
            }
            showActions={false}
          />
        </>
      );
    case tabName.log:
      return data.isLoading ? (
        <TableLoader />
      ) : windowOpts ? (
        <AutoSizer disableHeight>
          {({ width }) => (
            <List
              autoHeight
              deferredMeasurementCache={cache}
              height={windowOpts.windowHeight}
              rowCount={data.hostLog.length}
              rowHeight={cache.rowHeight}
              rowRenderer={rowRenderer}
              overscanRowCount={0}
              width={width}
              isScrolling={windowOpts.windowIsScrolling}
              onScroll={windowOpts.windowOnScroll}
              scrollTop={windowOpts.windowScrollTop}
            />
          )}
        </AutoSizer>
      ) : (
        <AutoSizer>
          {({ width, height }) => (
            <List
              deferredMeasurementCache={cache}
              height={height}
              rowCount={data.hostLog.length}
              rowHeight={cache.rowHeight}
              rowRenderer={rowRenderer}
              overscanRowCount={0}
              width={width}
            />
          )}
        </AutoSizer>
      );
    default:
  }
};

DataTable.propTypes = {
  data: PropTypes.shape({
    topicData: PropTypes.array,
    hostLog: PropTypes.array,
    isLoading: PropTypes.bool,
  }),
  type: PropTypes.string.isRequired,
  others: PropTypes.any,
};

export default DataTable;
