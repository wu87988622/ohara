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

import React, { useState, useImperativeHandle } from 'react';
import PropTypes from 'prop-types';
import { isEqual, sortedUniq } from 'lodash';

import { Dialog } from 'components/common/Dialog';
import FileTable from './FileTable';

const FileSelectorDialog = React.forwardRef((props, ref) => {
  const {
    dialogTitle,
    isOpen,
    files,
    onClose,
    onConfirm,
    onUpload,
    tableTitle,
  } = props;
  const [selectedFiles, setSelectedFiles] = useState(props.selectedFiles);

  const saveable = isEqual(
    sortedUniq(props.selectedFiles),
    sortedUniq(selectedFiles),
  );

  const handleSelectionChange = selectFiles => {
    setSelectedFiles(selectFiles);
  };

  const handleCancel = () => {
    setSelectedFiles(props.selectedFiles);
    onClose();
  };

  const handleConfirm = () => {
    onConfirm(selectedFiles);
  };

  useImperativeHandle(ref, () => ({
    setSelectedFiles,
  }));

  return (
    <Dialog
      title={dialogTitle}
      open={isOpen}
      handleClose={handleCancel}
      handleConfirm={handleConfirm}
      confirmDisabled={saveable}
      confirmText="Save"
      maxWidth="md"
    >
      <FileTable
        files={files}
        onSelectionChange={handleSelectionChange}
        onUpload={onUpload}
        options={{
          selectedFiles,
          selection: true,
          showDeleteIcon: false,
        }}
        title={tableTitle}
      />
    </Dialog>
  );
});

FileSelectorDialog.propTypes = {
  dialogTitle: PropTypes.string,
  isOpen: PropTypes.bool.isRequired,
  files: PropTypes.array,
  onClose: PropTypes.func,
  onConfirm: PropTypes.func,
  onUpload: PropTypes.func,
  selectedFiles: PropTypes.array,
  tableTitle: PropTypes.string,
};

FileSelectorDialog.defaultProps = {
  dialogTitle: 'Select files',
  files: [],
  onClose: () => {},
  onConfirm: () => {},
  onUpload: () => {},
  selectedFiles: [],
  tableTitle: 'Files',
};

export default FileSelectorDialog;
