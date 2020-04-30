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
import { MODE } from 'const';
import * as hooks from 'hooks';
import NodeTable from './NodeTable';

const NodeSelectorDialog = React.forwardRef((props, ref) => {
  const {
    dialogTitle,
    isOpen,
    mode,
    nodes,
    onClose,
    onConfirm,
    tableTitle,
  } = props;
  const createNode = hooks.useCreateNodeAction();
  const [selectedNodes, setSelectedNodes] = useState(props.selectedNodes);

  const saveable = isEqual(
    sortedUniq(props.selectedNodes),
    sortedUniq(selectedNodes),
  );

  const handleCreate = nodeToCreate => {
    return new Promise((resolve, reject) => {
      createNode(nodeToCreate, {
        onSuccess: () => resolve(),
        onError: error => reject(error),
      });
    });
  };

  const handleSelectionChange = selectNodes => {
    setSelectedNodes(selectNodes);
  };

  const handleCancel = () => {
    setSelectedNodes(props.selectedNodes);
    onClose();
  };

  const handleConfirm = () => {
    onConfirm(selectedNodes);
  };

  useImperativeHandle(ref, () => ({
    setSelectedNodes,
  }));

  return (
    <Dialog
      title={dialogTitle}
      open={isOpen}
      onClose={handleCancel}
      onConfirm={handleConfirm}
      confirmDisabled={saveable}
      confirmText="Save"
      maxWidth="md"
    >
      <NodeTable
        nodes={nodes}
        onCreate={handleCreate}
        onSelectionChange={handleSelectionChange}
        options={{
          mode,
          selectedNodes,
          selection: true,
          showCreateIcon: mode !== MODE.K8S,
          showDeleteIcon: false,
          showEditorIcon: false,
        }}
        title={tableTitle}
      />
    </Dialog>
  );
});

NodeSelectorDialog.propTypes = {
  dialogTitle: PropTypes.string,
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.string,
  nodes: PropTypes.array,
  onClose: PropTypes.func,
  onConfirm: PropTypes.func,
  selectedNodes: PropTypes.array,
  tableTitle: PropTypes.string,
};

NodeSelectorDialog.defaultProps = {
  dialogTitle: 'Select nodes',
  mode: MODE.K8S,
  nodes: [],
  onClose: () => {},
  onConfirm: () => {},
  selectedNodes: [],
  tableTitle: 'Nodes',
};

export default NodeSelectorDialog;
