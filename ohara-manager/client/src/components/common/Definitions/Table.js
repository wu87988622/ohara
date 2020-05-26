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
import MaterialTable from 'material-table';
import { forwardRef } from 'react';
import AddBox from '@material-ui/icons/AddBox';
import ArrowDownward from '@material-ui/icons/ArrowDownward';
import Check from '@material-ui/icons/Check';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import ChevronRight from '@material-ui/icons/ChevronRight';
import Clear from '@material-ui/icons/Clear';
import DeleteOutline from '@material-ui/icons/DeleteOutline';
import Edit from '@material-ui/icons/Edit';
import FilterList from '@material-ui/icons/FilterList';
import FirstPage from '@material-ui/icons/FirstPage';
import LastPage from '@material-ui/icons/LastPage';
import Remove from '@material-ui/icons/Remove';
import SaveAlt from '@material-ui/icons/SaveAlt';
import Search from '@material-ui/icons/Search';
import ViewColumn from '@material-ui/icons/ViewColumn';
import FormHelperText from '@material-ui/core/FormHelperText';

const tableIcons = {
  Add: forwardRef((props, ref) => <AddBox {...props} ref={ref} />),
  Check: forwardRef((props, ref) => <Check {...props} ref={ref} />),
  Clear: forwardRef((props, ref) => <Clear {...props} ref={ref} />),
  Delete: forwardRef((props, ref) => <DeleteOutline {...props} ref={ref} />),
  DetailPanel: forwardRef((props, ref) => (
    <ChevronRight {...props} ref={ref} />
  )),
  Edit: forwardRef((props, ref) => <Edit {...props} ref={ref} />),
  Export: forwardRef((props, ref) => <SaveAlt {...props} ref={ref} />),
  Filter: forwardRef((props, ref) => <FilterList {...props} ref={ref} />),
  FirstPage: forwardRef((props, ref) => <FirstPage {...props} ref={ref} />),
  LastPage: forwardRef((props, ref) => <LastPage {...props} ref={ref} />),
  NextPage: forwardRef((props, ref) => <ChevronRight {...props} ref={ref} />),
  PreviousPage: forwardRef((props, ref) => (
    <ChevronLeft {...props} ref={ref} />
  )),
  ResetSearch: forwardRef((props, ref) => <Clear {...props} ref={ref} />),
  Search: forwardRef((props, ref) => <Search {...props} ref={ref} />),
  SortArrow: forwardRef((props, ref) => <ArrowDownward {...props} ref={ref} />),
  ThirdStateCheck: forwardRef((props, ref) => <Remove {...props} ref={ref} />),
  ViewColumn: forwardRef((props, ref) => <ViewColumn {...props} ref={ref} />),
};

const typeConverter = key => {
  switch (key) {
    case 'number':
      return 'numeric';
    case 'boolean':
      return 'boolean';

    default:
      return 'string';
  }
};

const Table = props => {
  const {
    input: { name, onChange, value = [] },
    meta = {},
    helperText,
    label,
    tableKeys,
    refs,
  } = props;

  const stateRef = React.useRef({});

  stateRef.current = {
    columns: tableKeys.map(tableKey => {
      return {
        title: tableKey.name,
        field: tableKey.name,
        type: typeConverter(tableKey.type),
        ...(tableKey.recommendedValues.length > 0 && {
          lookup: { ...tableKey.recommendedValues },
        }),
      };
    }),
    data: [...value],
  };

  const hasError = (meta.error && meta.touched) || (meta.error && meta.dirty);
  return (
    <div ref={refs}>
      <MaterialTable
        options={{
          paging: false,
          draggable: false,
        }}
        name={name}
        icons={tableIcons}
        title={label}
        columns={stateRef.current.columns}
        data={stateRef.current.data}
        editable={{
          onRowAdd: newData =>
            new Promise(resolve => {
              setTimeout(() => {
                resolve();
                const newRow = () => {
                  let data = [...stateRef.current.data];
                  data.push(newData);
                  return data;
                };
                stateRef.current.data = newRow();
                onChange(stateRef.current.data);
              });
            }),
          onRowUpdate: (newData, oldData) =>
            new Promise(resolve => {
              setTimeout(() => {
                resolve();
                if (oldData) {
                  const newRow = () => {
                    const data = [...stateRef.current.data];
                    data[data.indexOf(oldData)] = newData;
                    return data;
                  };
                  stateRef.current.data = newRow();
                  onChange(stateRef.current.data);
                }
              });
            }),
          onRowDelete: oldData =>
            new Promise(resolve => {
              setTimeout(() => {
                resolve();
                const newRow = () => {
                  const data = [...stateRef.current.data];
                  data.splice(data.indexOf(oldData), 1);
                  return data;
                };
                stateRef.current.data = newRow();
                onChange(stateRef.current.data);
              });
            }),
        }}
      />
      <FormHelperText children={hasError ? meta.error : helperText} />
    </div>
  );
};

Table.propTypes = {
  input: PropTypes.shape({
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
      PropTypes.object,
      PropTypes.array,
    ]).isRequired,
  }).isRequired,
  meta: PropTypes.shape({
    dirty: PropTypes.bool,
    touched: PropTypes.bool,
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  }),
  helperText: PropTypes.string,
  label: PropTypes.string,
  tableKeys: PropTypes.array,
  refs: PropTypes.object,
};
Table.defaultProps = {
  helperText: '',
};

export default Table;
