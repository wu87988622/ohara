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
import Paper from '@material-ui/core/Paper';
import Table from '@material-ui/core/Table';
import TableRow from '@material-ui/core/TableRow';
import TableHead from '@material-ui/core/TableHead';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableSortLabel from '@material-ui/core/TableSortLabel';

import { TableLoader } from 'components/common/Mui/Loader';

const SortTable = props => {
  const { isLoading, order, orderBy, onRequestSort, headRows, rows } = props;
  const createSortHandler = property => event => {
    onRequestSort(event, property);
  };

  const desc = (a, b, orderBy) => {
    if (b[orderBy] < a[orderBy]) {
      return -1;
    }
    if (b[orderBy] > a[orderBy]) {
      return 1;
    }
    return 0;
  };

  const stableSort = (array, cmp) => {
    const stabilizedThis = array.map((el, index) => [el, index]);
    stabilizedThis.sort((a, b) => {
      const order = cmp(a[0], b[0]);
      if (order !== 0) return order;
      return a[1] - b[1];
    });
    return stabilizedThis.map(el => el[0]);
  };

  const getSorting = (order, orderBy) => {
    return order === 'desc'
      ? (a, b) => desc(a, b, orderBy)
      : (a, b) => -desc(a, b, orderBy);
  };

  if (isLoading) return <TableLoader />;

  return (
    <Paper>
      <Table>
        <TableHead>
          <TableRow>
            {headRows.map(row => {
              const { id, label, sort = true } = row;
              const align = id === headRows[0].id ? 'left' : 'right';
              return (
                <React.Fragment key={id}>
                  {sort ? (
                    <TableCell
                      align={align}
                      sortDirection={orderBy === id ? order : false}
                    >
                      <TableSortLabel
                        active={orderBy === id}
                        direction={order}
                        onClick={createSortHandler(id)}
                      >
                        {label}
                      </TableSortLabel>
                    </TableCell>
                  ) : (
                    <TableCell align={align}>{label}</TableCell>
                  )}
                </React.Fragment>
              );
            })}
          </TableRow>
        </TableHead>
        <TableBody>
          {stableSort(rows, getSorting(order, orderBy)).map(row => {
            const keys = Object.keys(row);
            return (
              <TableRow key={row[keys[0]]}>
                {keys.map(key => {
                  return (
                    <TableCell
                      key={row[key]}
                      align={key === keys[0] ? 'left' : 'right'}
                    >
                      {row[key]}
                    </TableCell>
                  );
                })}
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </Paper>
  );
};

SortTable.propTypes = {
  order: PropTypes.string.isRequired,
  orderBy: PropTypes.string.isRequired,
  onRequestSort: PropTypes.func.isRequired,
  headRows: PropTypes.array.isRequired,
  rows: PropTypes.array.isRequired,
  isLoading: PropTypes.bool.isRequired,
};

export default SortTable;
