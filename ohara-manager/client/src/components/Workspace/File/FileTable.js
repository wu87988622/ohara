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

import React, { useEffect, useState } from 'react';
import { get } from 'lodash';
import Grid from '@material-ui/core/Grid';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';
import NumberFormat from 'react-number-format';

import { useWorkspace, useFileState, useFileActions } from 'context';
import { Table } from 'components/common/Table';
import { QuickSearch } from 'components/common/Search';
import FileActionsMenu from './FileActionsMenu';
import { getDateFromTimestamp } from 'utils/date';
import { Wrapper } from './FileTableStyles';

const FileTable = () => {
  const { currentWorkspace } = useWorkspace();
  const { data: files } = useFileState();
  const { fetchFiles } = useFileActions();
  const [filteredFiles, setFilteredFiles] = useState([]);

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchFiles(currentWorkspace.settings.name);
  }, [fetchFiles, currentWorkspace]);

  const tableHeaders = [
    'Name',
    'Used by',
    'File size(KB)',
    'Last modified',
    'Actions',
  ];

  return (
    <Wrapper>
      <Grid container className="actions">
        <QuickSearch
          data={files}
          keys={['name', 'size']}
          setResults={setFilteredFiles}
        />
      </Grid>
      <Table headers={tableHeaders} title="All Files">
        {filteredFiles.map(file => {
          const name = get(file, 'name', '');
          const size = get(file, 'size', 0);
          const lastModified = getDateFromTimestamp(get(file, 'lastModified'));
          return (
            <TableRow key={name}>
              <TableCell>{name}</TableCell>
              <TableCell></TableCell>
              <TableCell>
                <NumberFormat
                  value={size}
                  displayType="text"
                  thousandSeparator
                />
              </TableCell>
              <TableCell>{lastModified}</TableCell>
              <TableCell align="right">
                <FileActionsMenu file={file} />
              </TableCell>
            </TableRow>
          );
        })}
      </Table>
    </Wrapper>
  );
};

export default FileTable;
