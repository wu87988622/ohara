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
import { capitalize, includes, toUpper } from 'lodash';

import { Dialog } from 'components/common/Dialog';
import Table from 'components/common/Table/MuiTable';

function FileClassInfoDialog(props) {
  const customizeColumn = key => {
    return {
      title: capitalize(key),
      customFilterAndSearch: (filterValue, rowData) => {
        const value = rowData?.settingDefinitions?.find(
          definition => definition?.key === key,
        )?.defaultValue;
        return includes(toUpper(value), toUpper(filterValue));
      },
      render: rowData => {
        return rowData?.settingDefinitions?.find(
          definition => definition?.key === key,
        )?.defaultValue;
      },
    };
  };

  return (
    <Dialog
      maxWidth="md"
      onClose={props.onClose}
      open={props.isOpen}
      showActions={false}
      title={`View ${props.file?.name} file`}
    >
      <Table
        columns={[
          { title: 'Type', field: 'classType' },
          { title: 'Name', field: 'className' },
          customizeColumn('author'),
          customizeColumn('version'),
          customizeColumn('revision'),
        ]}
        data={props.file?.classInfos || []}
        options={{
          selection: false,
          rowStyle: null,
        }}
        title="Class infos"
      />
    </Dialog>
  );
}

FileClassInfoDialog.propTypes = {
  file: PropTypes.shape({
    name: PropTypes.string,
    classInfos: PropTypes.arrayOf(
      PropTypes.shape({
        className: PropTypes.string,
        classType: PropTypes.string,
        settingDefinitions: PropTypes.arrayOf(
          PropTypes.shape({
            key: PropTypes.string,
            defaultValue: PropTypes.oneOfType([
              PropTypes.bool,
              PropTypes.number,
              PropTypes.string,
            ]),
          }),
        ),
      }),
    ),
  }),
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
};

FileClassInfoDialog.defaultProps = {
  file: {},
  onClose: () => {},
};

export default FileClassInfoDialog;
