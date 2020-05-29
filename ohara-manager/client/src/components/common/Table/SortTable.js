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

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import Paper from '@material-ui/core/Paper';
import Table from '@material-ui/core/Table';
import TableRow from '@material-ui/core/TableRow';
import TableHead from '@material-ui/core/TableHead';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableSortLabel from '@material-ui/core/TableSortLabel';

import { TableLoader } from 'components/common/Loader';
import TableToolbar from './TableToolbar';

const SortTable = props => {
  const {
    isLoading,
    headRows,
    rows,
    defaultOrder = 'asc',
    defaultOrderBy = headRows[0].id,
    tableName = 'sort',
    dataRowTestId = 'sort-list',
    buttonTestId = 'sort-button',
    handleDiscard,
    handleRestart,
  } = props;

  const [order, setOrder] = useState(defaultOrder);
  const [orderBy, setOrderBy] = useState(defaultOrderBy);

  const handleRequestSort = (e, property) => {
    const isDesc = orderBy === property && order === 'desc';
    setOrder(isDesc ? 'asc' : 'desc');
    setOrderBy(property);
  };

  const createSortHandler = property => event => {
    handleRequestSort(event, property);
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

  const stableSort = (array, component) => {
    const stabilizedThis = array.map((el, index) => [el, index]);
    stabilizedThis.sort((a, b) => {
      const order = component(a[0], b[0]);
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

  let newCount = 0;
  let deleteCount = 0;

  rows.forEach(row => {
    switch (row.type) {
      case 'ADD': {
        newCount++;
        break;
      }
      case 'DELETE': {
        deleteCount++;
        break;
      }
      default: {
        break;
      }
    }
  });

  return (
    <Paper>
      {newCount > 0 || deleteCount > 0 ? (
        <TableToolbar
          deleteCount={deleteCount}
          handleDiscard={handleDiscard}
          handleRestart={handleRestart}
          newCount={newCount}
        />
      ) : (
        <></>
      )}
      <Table>
        <TableHead>
          <TableRow>
            {headRows.map((row, i, arr) => {
              const { id, label, sortable = true } = row;
              const align = arr.length - 1 === i ? 'right' : 'left';
              return (
                <React.Fragment key={id}>
                  {sortable ? (
                    <TableCell
                      align={align}
                      sortDirection={orderBy === id ? order : false}
                    >
                      <TableSortLabel
                        active={orderBy === id}
                        data-testid={buttonTestId}
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
          {stableSort(rows, getSorting(order, orderBy)).map((row, index) => {
            const keys = Object.keys(row);

            const StyledTableRow = styled(TableRow)`
              background-color: ${props => {
                switch (row.type) {
                  case 'ADD': {
                    // eslint-disable-next-line react/prop-types
                    return props.theme.palette.primary.highlight;
                  }
                  case 'DELETE': {
                    // eslint-disable-next-line react/prop-types
                    return props.theme.palette.action.selected;
                  }
                  default: {
                    return;
                  }
                }
              }};
            `;
            return (
              // Using map method's index as the key since our UI
              // allows same name item in the table, so it's not
              // guaranteed `name` is unique and React would have
              // problem rendering it
              <StyledTableRow data-testid={dataRowTestId} key={index}>
                {keys
                  .filter(key => key !== 'type')
                  .map((key, i, arr) => {
                    return (
                      <TableCell
                        align={arr.length - 1 === i ? 'right' : 'left'}
                        data-testid={`${tableName}-${key}`}
                        key={`${key}:${row[key]}`}
                      >
                        {row[key]}
                      </TableCell>
                    );
                  })}
              </StyledTableRow>
            );
          })}
        </TableBody>
      </Table>
    </Paper>
  );
};

SortTable.propTypes = {
  headRows: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      label: PropTypes.string,
    }),
  ),
  rows: PropTypes.array.isRequired,
  isLoading: PropTypes.bool.isRequired,
  tableName: PropTypes.string,
  defaultOrder: PropTypes.string,
  defaultOrderBy: PropTypes.string,
  dataRowTestId: PropTypes.string,
  buttonTestId: PropTypes.string,
  handleDiscard: PropTypes.func,
  handleRestart: PropTypes.func,
};

export default SortTable;
