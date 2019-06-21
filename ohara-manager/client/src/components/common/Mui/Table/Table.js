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
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Paper from '@material-ui/core/Paper';

import { TableLoader } from 'components/common/Mui/Loader';

const MuiTable = props => {
  const { headers, isLoading, children } = props;

  const lastIdx = headers.length - 1;

  if (isLoading) return <TableLoader />;

  return (
    <Paper>
      <Table>
        <TableHead>
          <TableRow>
            {headers.map((header, idx) => {
              const align = idx === lastIdx ? 'right' : 'left';
              return (
                <TableCell align={align} key={header}>
                  {header}
                </TableCell>
              );
            })}
          </TableRow>
        </TableHead>
        <TableBody>{children()}</TableBody>
      </Table>
    </Paper>
  );
};

MuiTable.propTypes = {
  headers: PropTypes.arrayOf(PropTypes.string).isRequired,
  isLoading: PropTypes.bool.isRequired,
  children: PropTypes.func.isRequired,
};

export default MuiTable;
