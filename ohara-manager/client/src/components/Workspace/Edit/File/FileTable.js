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

import React, { useState, useEffect } from 'react';
import Grid from '@material-ui/core/Grid';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';
import NumberFormat from 'react-number-format';
import CheckIcon from '@material-ui/icons/Check';

import * as hooks from 'hooks';
import { Table } from 'components/common/Table';
import { QuickSearch } from 'components/common/Search';
import FileActionsMenu from './FileActionsMenu';
import { Wrapper } from './FileTableStyles';

const tableHeaders = [
  'Name',
  'Used',
  'File size(KB)',
  'Last modified',
  'Actions',
];

const FileTable = () => {
  const files = hooks.useFiles();
  const fetchFiles = hooks.useFetchFilesAction();
  const isLoaded = hooks.useIsFileLoaded();
  const [filteredFiles, setFilteredFiles] = useState([]);

  useEffect(() => {
    if (!isLoaded) fetchFiles();
  }, [fetchFiles, isLoaded]);

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
          return (
            <TableRow key={file.name}>
              <TableCell>{file.name}</TableCell>
              <TableCell>
                {file.isUsed && <CheckIcon className="checkIcon" />}
              </TableCell>
              <TableCell>
                <NumberFormat
                  value={file.size}
                  displayType="text"
                  thousandSeparator
                />
              </TableCell>
              <TableCell>{file.lastModified}</TableCell>
              <TableCell align="right">
                <FileActionsMenu file={file} deleteDisabled={file.isUsed} />
              </TableCell>
            </TableRow>
          );
        })}
      </Table>
    </Wrapper>
  );
};

export default FileTable;
