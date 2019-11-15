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
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';
import Paper from '@material-ui/core/Paper';
import Checkbox from '@material-ui/core/Checkbox';

import Prcentage from '../Progress/Prcentage';

const SelectTableHead = props => {
  const {
    onSelectAllClick,
    numSelected,
    rowCount,
    headCells,
    hasSelect,
  } = props;

  return (
    <TableHead style={{ background: '#f5f6fa' }}>
      <TableRow>
        {hasSelect && (
          <TableCell padding="checkbox">
            <Checkbox
              indeterminate={numSelected > 0 && numSelected < rowCount}
              checked={numSelected === rowCount}
              onChange={onSelectAllClick}
              color="primary"
            />
          </TableCell>
        )}
        {headCells.map((headCell, i, arr) => {
          const align = arr.length - 1 === i ? 'right' : 'left';
          return (
            <TableCell
              key={headCell.id}
              align={align}
              padding={headCell.disablePadding ? 'none' : 'default'}
            >
              {headCell.label}
            </TableCell>
          );
        })}
      </TableRow>
    </TableHead>
  );
};

const SelectTableToolbar = props => {
  const { numSelected, title, hasSelect } = props;

  return (
    <Toolbar>
      {numSelected > 0 && hasSelect ? (
        <Typography color="inherit" variant="subtitle1">
          {numSelected} selected
        </Typography>
      ) : (
        <Typography variant="h6">{title}</Typography>
      )}
    </Toolbar>
  );
};

const SelectTable = props => {
  const {
    rows,
    headCells,
    title,
    hasSelect = false,
    selected = [],
    setSelected,
  } = props;

  const handleSelectAllClick = event => {
    if (event.target.checked) {
      setSelected(rows);
      return;
    }
    setSelected([]);
  };

  const handleClick = (event, row) => {
    const selectedIndex = selected
      .map(select => select[Object.keys(select)[0]])
      .indexOf(row[Object.keys(row)[0]]);
    let newSelected = [];
    if (selectedIndex === -1) {
      newSelected = newSelected.concat(selected, row);
    } else if (selectedIndex === 0) {
      newSelected = newSelected.concat(selected.slice(1));
    } else if (selectedIndex === selected.length - 1) {
      newSelected = newSelected.concat(selected.slice(0, -1));
    } else if (selectedIndex > 0) {
      newSelected = newSelected.concat(
        selected.slice(0, selectedIndex),
        selected.slice(selectedIndex + 1),
      );
    }

    setSelected(newSelected);
  };

  const isSelected = row =>
    selected
      .map(select => select[Object.keys(select)[0]])
      .indexOf(row[Object.keys(row)[0]]) !== -1;

  return (
    <div>
      <Paper>
        <SelectTableToolbar
          numSelected={selected.length}
          title={title}
          hasSelect={hasSelect}
        />
        <div>
          <Table size="medium">
            <SelectTableHead
              numSelected={selected.length}
              onSelectAllClick={handleSelectAllClick}
              rowCount={rows.length}
              headCells={headCells}
              hasSelect={hasSelect}
            />
            <TableBody>
              {rows.map(row => {
                const keys = Object.keys(row);
                const isItemSelected = isSelected(row);

                return (
                  <TableRow
                    hover
                    onClick={event => handleClick(event, row)}
                    role="checkbox"
                    aria-checked={isItemSelected}
                    tabIndex={-1}
                    key={row[keys[0]]}
                    selected={isItemSelected}
                  >
                    {hasSelect && (
                      <TableCell padding="checkbox">
                        <Checkbox checked={isItemSelected} color="primary" />
                      </TableCell>
                    )}
                    {keys
                      .filter(key => key !== 'type')
                      .map((key, i, arr) => {
                        return (
                          <TableCell
                            key={`${key}:${row[key]}`}
                            align={arr.length - 1 === i ? 'right' : 'left'}
                          >
                            {Prcentage(row[key], '|')}
                          </TableCell>
                        );
                      })}
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      </Paper>
    </div>
  );
};

SelectTableHead.propTypes = {
  onSelectAllClick: PropTypes.func.isRequired,
  numSelected: PropTypes.number.isRequired,
  rowCount: PropTypes.number.isRequired,
  headCells: PropTypes.array.isRequired,
  hasSelect: PropTypes.bool.isRequired,
};

SelectTableToolbar.propTypes = {
  numSelected: PropTypes.number.isRequired,
  title: PropTypes.string.isRequired,
  hasSelect: PropTypes.bool.isRequired,
};

SelectTable.propTypes = {
  rows: PropTypes.array.isRequired,
  headCells: PropTypes.array.isRequired,
  title: PropTypes.string.isRequired,
  hasSelect: PropTypes.bool,
  selected: PropTypes.array,
  setSelected: PropTypes.func,
};
export default SelectTable;
