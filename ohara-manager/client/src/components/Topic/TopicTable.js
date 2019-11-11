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
import { useParams } from 'react-router-dom';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';

import { useWorkspace } from 'context/WorkspaceContext';
import { Table } from 'components/common/Table';
import { useTopic } from 'context/TopicContext';
import { Button } from 'components/common/Form';

function TopicTable() {
  const { findByWorkspaceName } = useWorkspace();
  const { workspaceName } = useParams();
  const { topics, doFetch: fetchTopics } = useTopic();
  const currentWorkspace = findByWorkspaceName(workspaceName);

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchTopics(currentWorkspace.settings.name);
  }, [fetchTopics, currentWorkspace.settings.name, currentWorkspace]);

  const tableHeaders = [
    'Name',
    'Partitions',
    'Replications',
    'Used by pipelines',
    'Type',
    'Actions',
  ];
  return (
    <>
      <Table headers={tableHeaders} title="All Topics">
        {topics.map(topic => (
          <TableRow key={topic.settings.name}>
            <TableCell>{topic.settings.name}</TableCell>
            <TableCell>{topic.settings.numberOfPartitions}</TableCell>
            <TableCell>{topic.settings.numberOfReplications}</TableCell>
            <TableCell></TableCell>
            <TableCell></TableCell>
            <TableCell align="right">
              <Button variant="outlined" color="primary">
                View
              </Button>
            </TableCell>
          </TableRow>
        ))}
      </Table>
    </>
  );
}

export default TopicTable;
