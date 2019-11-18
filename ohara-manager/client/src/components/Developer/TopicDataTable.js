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

import { isEmpty } from 'lodash';
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import VisibilityIcon from '@material-ui/icons/Visibility';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';

import { Table } from 'components/common/Table';
import { Dialog } from 'components/common/Dialog';
import { DialogBody, FooterRow } from './Styles';

const TopicDataTable = props => {
  const [viewTopicMessage, setViewTopicMessage] = useState({});

  const { topicData, isLoadedTopic } = props;

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
    // the detail view header
    headers.sort().unshift('');
    return headers;
  };

  const genDataBody = topicData => {
    const headers = getHeaders(topicData);
    // we skip the detail view header
    headers.shift();
    return topicData.map((message, rowIdx) => (
      <TableRow key={rowIdx}>
        <TableCell key={rowIdx + '_view'}>
          <VisibilityIcon
            onClick={() => {
              setViewTopicMessage(message);
            }}
          />
        </TableCell>
        {message.value ? (
          headers.map((header, headerIdx) =>
            message.value[header] ? (
              <TableCell key={rowIdx + '_' + headerIdx}>
                {message.value[header]}
              </TableCell>
            ) : (
              <TableCell key={rowIdx + '_' + headerIdx} />
            ),
          )
        ) : (
          <TableCell key={rowIdx + '_error'}>{message.error}</TableCell>
        )}
      </TableRow>
    ));
  };

  return (
    <>
      <Table
        headers={getHeaders(topicData)}
        isLoading={isLoadedTopic}
        children={genDataBody(topicData)}
        footer={
          isEmpty(topicData) ? null : (
            <FooterRow>
              <TableCell>${topicData.length} rows per query</TableCell>
            </FooterRow>
          )
        }
      />
      <Dialog
        title="View topic source"
        open={!isEmpty(viewTopicMessage)}
        handleClose={() => setViewTopicMessage({})}
        children={
          <DialogBody>
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
          </DialogBody>
        }
        showActions={false}
      />
    </>
  );
};

TopicDataTable.propTypes = {
  topicData: PropTypes.array.isRequired,
  isLoadedTopic: PropTypes.bool,
};

export default TopicDataTable;
