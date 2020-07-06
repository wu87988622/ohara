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

import React, { useMemo, useState, useImperativeHandle } from 'react';
import PropTypes from 'prop-types';
import { isEmpty, xorBy } from 'lodash';

import { Dialog } from 'components/common/Dialog';
import NodeTable from './NodeTable';

const NodeSelectorDialog = React.forwardRef((props, ref) => {
  const { isOpen, onClose, onConfirm, tableProps, testId, title } = props;

  const initialSelectedNodes = tableProps?.options?.selectedNodes || [];

  const [selectedNodes, setSelectedNodes] = useState(initialSelectedNodes);

  const handleSelectionChange = (selectNodes) => {
    setSelectedNodes(selectNodes);
  };

  const handleCancel = () => {
    setSelectedNodes(initialSelectedNodes);
    onClose();
  };

  const handleConfirm = () => {
    if (typeof onConfirm === 'function') {
      onConfirm(selectedNodes);
    }
  };

  useImperativeHandle(ref, () => ({
    setSelectedNodes,
  }));

  const confirmDisabled = useMemo(() => {
    let disabled = false;
    if (isEmpty(selectedNodes)) {
      disabled = true;
    }
    if (xorBy(selectedNodes, initialSelectedNodes, 'hostname').length === 0) {
      disabled = true;
    }
    return disabled;
  }, [selectedNodes, initialSelectedNodes]);

  const confirmTooltip = useMemo(() => {
    let tooltip = null;
    if (isEmpty(selectedNodes)) {
      tooltip = 'Must select more than one node';
    }
    if (xorBy(selectedNodes, initialSelectedNodes, 'hostname').length === 0) {
      tooltip = 'No change';
    }
    return tooltip;
  }, [selectedNodes, initialSelectedNodes]);

  return (
    <Dialog
      confirmDisabled={confirmDisabled}
      confirmText="SAVE"
      confirmTooltip={confirmTooltip}
      maxWidth="md"
      onClose={handleCancel}
      onConfirm={handleConfirm}
      open={isOpen}
      testId={testId}
      title={title}
    >
      <NodeTable
        {...tableProps}
        onSelectionChange={handleSelectionChange}
        options={{
          selection: true,
          showCreateIcon: false,
          showDeleteIcon: false,
          showEditorIcon: false,
          ...tableProps?.options,
        }}
      />
    </Dialog>
  );
});

NodeSelectorDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
  onConfirm: PropTypes.func,
  tableProps: PropTypes.shape({
    options: PropTypes.shape({
      selectedNodes: PropTypes.array,
    }),
  }),
  testId: PropTypes.string,
  title: PropTypes.string,
};

NodeSelectorDialog.defaultProps = {
  onClose: () => {},
  onConfirm: () => {},
  tableProps: {},
  title: 'Select node',
};

export default NodeSelectorDialog;
