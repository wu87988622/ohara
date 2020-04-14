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

import React, { useRef } from 'react';

import CheckIcon from '@material-ui/icons/Check';
import AddBox from '@material-ui/icons/AddBox';
import NumberFormat from 'react-number-format';

import * as hooks from 'hooks';
import Table from 'components/common/Table/MuiTable';
import FileActionsMenu from './FileActionsMenu';
import FileUpload from './FileUpload';

const FileList = () => {
  const files = hooks.useFiles();
  const fileUploadRef = useRef(null);

  return (
    <>
      <Table
        actions={[
          {
            icon: () => <AddBox />,
            tooltip: 'Upload File',
            isFreeAction: true,
            onClick: () => {
              fileUploadRef.current.click();
            },
          },
        ]}
        columns={[
          { title: 'Name', field: 'name' },
          {
            title: 'Used',
            render: file => {
              if (file.isUsed) return <CheckIcon className="checkIcon" />;
            },
          },
          {
            title: 'File size(KB)',
            type: 'numeric',
            field: 'size',
            render: file => (
              <NumberFormat
                value={file.size}
                displayType="text"
                thousandSeparator
              />
            ),
          },

          { title: 'Last modified', type: 'date', field: 'lastModified' },
          {
            title: 'Actions',
            cellStyle: { textAlign: 'right' },
            headerStyle: { textAlign: 'right' },
            sorting: false,
            render: file => (
              <FileActionsMenu file={file} deleteDisabled={file.isUsed} />
            ),
          },
        ]}
        data={files}
        options={{
          paging: false,
          search: true,
          searchFieldAlignment: 'left',
          showTitle: false,
        }}
      />
      <FileUpload ref={fileUploadRef} />
    </>
  );
};

export default FileList;
