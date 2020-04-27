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
import { assign, get, isString, isFunction, reject, size, some } from 'lodash';
import Table from 'material-table';
import Checkbox from '@material-ui/core/Checkbox';
import MuiTableIcons from './MuiTableIcons';

const defaultProps = {
  onSelectionChange: () => {},
  options: {
    paging: false,
    predicate: 'name', // or function predicate(rowData) { return rowData.name; }
    search: true,
    selection: false,
    selectedData: [],
  },
};

const MuiTable = props => {
  const { columns, data, onSelectionChange, options, ...restProps } = props;
  const { predicate, selection, selectedData, ...restOptions } = assign(
    defaultProps.options,
    options,
  );
  const [selectedRows, setSelectedRows] = useState(selectedData || []);

  const dataCount = size(data);
  const selectedCount = size(selectedRows);

  const finalPredicate = isFunction(predicate)
    ? predicate
    : object => get(object, predicate);

  const isEqual = (object, otherObject) => {
    const value = finalPredicate(object);
    const otherValue = finalPredicate(otherObject);
    return isString(value) && isString(otherValue) && value === otherValue;
  };

  const handleRowSelected = (event, rowData) => {
    if (rowData) {
      const shouldBeRemoved = some(selectedRows, selectedRowData =>
        isEqual(selectedRowData, rowData),
      );

      const newSelectedRows = shouldBeRemoved
        ? reject(selectedRows, selectedRowData =>
            isEqual(selectedRowData, rowData),
          )
        : [...selectedRows, rowData];

      setSelectedRows(newSelectedRows);
      onSelectionChange(newSelectedRows, rowData);
    }
    event.stopPropagation();
  };

  const handleAllSelected = (_, checked) => {
    setSelectedRows(checked ? data : []);
    onSelectionChange(checked ? data : []);
  };

  const renderSelectionColumn = () => {
    const style = { paddingLeft: '0px', paddingRight: '0px', width: '42px' };
    return {
      cellStyle: style,
      headerStyle: style,
      hidden: !selection,
      render: rowData => (
        <Checkbox
          checked={some(selectedRows, selectedRowData =>
            isEqual(selectedRowData, rowData),
          )}
          color="primary"
          onChange={event => handleRowSelected(event, rowData)}
        />
      ),
      sorting: false,
      title: (
        <Checkbox
          checked={dataCount > 0 && selectedCount === dataCount}
          color="primary"
          indeterminate={selectedCount > 0 && selectedCount < dataCount}
          onChange={handleAllSelected}
        />
      ),
    };
  };

  return (
    <Table
      {...restProps}
      columns={[renderSelectionColumn(), ...columns]}
      data={data}
      options={{ ...restOptions }}
      icons={MuiTableIcons}
    />
  );
};

MuiTable.propTypes = {
  columns: PropTypes.array.isRequired,
  data: PropTypes.array.isRequired,
  onSelectionChange: PropTypes.func,
  options: PropTypes.shape({
    paging: PropTypes.bool,
    predicate: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
    search: PropTypes.bool,
    selection: PropTypes.bool,
    selectedData: PropTypes.array,
  }),
};

MuiTable.defaultProps = defaultProps;

export default MuiTable;
