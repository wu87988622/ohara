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
import WorkspaceFileTable from './WorkspaceFileTable';

const WorkspaceFileSelectorDialog = React.forwardRef((props, ref) => {
  const { isOpen, onClose, onConfirm, tableProps, title } = props;
  const [selectedFiles, setSelectedFiles] = useState(
    tableProps?.options?.selectedFiles,
  );

  const saveable = isEqual(
    sortedUniq(selectedFiles),
    sortedUniq(tableProps?.options?.selectedFiles),
  );

  const handleSelectionChange = selectFiles => {
    setSelectedFiles(selectFiles);
  };

  const handleCancel = () => {
    setSelectedFiles(tableProps?.options?.selectedFiles);
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
      confirmDisabled={saveable}
      confirmText="Save"
      maxWidth="md"
      onClose={handleCancel}
      onConfirm={handleConfirm}
      open={isOpen}
      title={title}
    >
      <WorkspaceFileTable
        onSelectionChange={handleSelectionChange}
        options={{
          selection: true,
          selectedFiles: tableProps?.options?.selectedFiles,
        }}
      />
    </Dialog>
  );
});

WorkspaceFileSelectorDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
  onConfirm: PropTypes.func,
  tableProps: PropTypes.shape({
    options: PropTypes.shape({
      selectedFiles: PropTypes.array,
    }),
  }),
  title: PropTypes.string,
};

WorkspaceFileSelectorDialog.defaultProps = {
  onClose: () => {},
  onConfirm: () => {},
  tableProps: {},
  title: 'Select file',
};

export default WorkspaceFileSelectorDialog;
