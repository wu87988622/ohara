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
import { get, isEmpty, capitalize } from 'lodash';
import VisibilityIcon from '@material-ui/icons/Visibility';
import ReactJson from 'react-json-view';

import * as hooks from 'hooks';
import { Table } from 'components/common/Table';
import { Dialog } from 'components/common/Dialog';
import {
  StyledTableRow,
  StyledTableCell,
  StyledTableErrorCell,
} from './ViewStyles';

const ViewTopic = () => {
  const [viewTopicMessage, setViewTopicMessage] = React.useState({});

  const { data, isFetching } = hooks.useDevToolTopicData();
  const messages = get(data, 'messages', []);

  const getHeaders = messages => {
    let headers = [];
    messages.forEach(message => {
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

  const renderDataBody = topicData => {
    const headers = getHeaders(topicData);
    // we skip the detail view header
    headers.shift();

    const lastItem = headers.length - 1;
    return topicData.map((message, rowIdx) => {
      return (
        <StyledTableRow key={rowIdx}>
          {message.value ? (
            <>
              <StyledTableCell key={rowIdx + '_view'}>
                <VisibilityIcon
                  data-testid={`detail-view-icon-${rowIdx}`}
                  onClick={() => {
                    setViewTopicMessage(message);
                  }}
                />
              </StyledTableCell>
              {headers.map((header, headerIdx) => {
                const align = headerIdx === lastItem ? 'right' : 'left';
                return message.value[header] ? (
                  <StyledTableCell align={align} key={rowIdx + '_' + headerIdx}>
                    {message.value[header]}
                  </StyledTableCell>
                ) : (
                  <StyledTableCell
                    align={align}
                    key={rowIdx + '_' + headerIdx}
                  />
                );
              })}
            </>
          ) : (
            <>
              <StyledTableErrorCell key={rowIdx + '_view'}>
                <VisibilityIcon
                  onClick={() => {
                    setViewTopicMessage(message);
                  }}
                />
              </StyledTableErrorCell>
              <StyledTableErrorCell
                className="error-message"
                colSpan={headers.length}
                key={rowIdx + '_error'}
              >
                {message.error}
              </StyledTableErrorCell>
            </>
          )}
        </StyledTableRow>
      );
    });
  };

  return (
    <>
      <Table
        fixedHeader
        headers={getHeaders(messages).map(header => capitalize(header))}
        isLoading={isFetching}
        children={renderDataBody(messages)}
        testId="view-topic-table"
      />
      <Dialog
        title="View topic source"
        open={!isEmpty(viewTopicMessage)}
        onClose={() => setViewTopicMessage({})}
        children={
          <ReactJson
            src={get(
              viewTopicMessage,
              'value',
              get(viewTopicMessage, 'error', {}),
            )}
            enableClipboard={false}
            iconStyle="square"
            displayDataTypes={false}
            displayObjectSize={false}
          />
        }
        showActions={false}
        testId="topic-detail-view"
      />
    </>
  );
};

export { ViewTopic };
