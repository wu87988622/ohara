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

import React, { useEffect } from 'react';
import { get } from 'lodash';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';

import {
  useWorkspace,
  useViewTopicDialog,
  useTopicState,
  useTopicActions,
} from 'context';
import { Table } from 'components/common/Table';
import { Button } from 'components/common/Form';
import ViewTopicDialog from './ViewTopicDialog';

function TopicTable() {
  const {
    open: openViewTopicDialog,
    setData: setViewTopicDialogData,
  } = useViewTopicDialog();

  const { currentWorkspace } = useWorkspace();
  const { data: topics } = useTopicState();
  const { fetchTopics } = useTopicActions();

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchTopics(currentWorkspace.settings.name);
  }, [fetchTopics, currentWorkspace]);

  const tableHeaders = [
    'Name',
    'Partitions',
    'Replications',
    'Used by pipelines',
    'Type',
    'State',
    'Actions',
  ];
  return (
    <>
      <Table headers={tableHeaders} title="All Topics">
        {topics.map(topic => (
          <TableRow key={get(topic, 'settings.name', '')}>
            <TableCell>{get(topic, 'settings.name', '')}</TableCell>
            <TableCell>
              {get(topic, 'settings.numberOfPartitions', 0)}
            </TableCell>
            <TableCell>
              {get(topic, 'settings.numberOfReplications', 0)}
            </TableCell>
            <TableCell></TableCell>
            <TableCell></TableCell>
            <TableCell>{get(topic, 'state', 'Unknown')}</TableCell>
            <TableCell align="right">
              <Button
                variant="outlined"
                color="primary"
                onClick={() => {
                  openViewTopicDialog();
                  setViewTopicDialogData(topic);
                }}
              >
                View
              </Button>
            </TableCell>
          </TableRow>
        ))}
      </Table>
      <ViewTopicDialog />
    </>
  );
}

export default TopicTable;
