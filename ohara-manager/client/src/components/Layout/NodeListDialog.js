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

import { FullScreenDialog } from 'components/common/Dialog';
import { NodeTable } from 'components/Node';
import { MODE } from 'const';
import * as hooks from 'hooks';

const NodeListDialog = props => {
  const { isOpen, mode, nodes, onClose } = props;

  const createNode = hooks.useCreateNodeAction();
  const deleteNode = hooks.useDeleteNodeAction();
  const updateNode = hooks.useUpdateNodeAction();

  const handleCreate = nodeToCreate => {
    return new Promise((resolve, reject) => {
      createNode(nodeToCreate, {
        onSuccess: () => resolve(),
        onError: error => reject(error),
      });
    });
  };

  const handleDelete = nodeToDelete => {
    deleteNode(nodeToDelete?.hostname);
  };

  const handleUpdate = nodeToUpdate => {
    updateNode(nodeToUpdate);
  };

  return (
    <FullScreenDialog title="All nodes" open={isOpen} onClose={onClose}>
      <NodeTable
        nodes={nodes}
        onCreate={handleCreate}
        onDelete={handleDelete}
        onUpdate={handleUpdate}
        options={{
          mode,
          selection: false,
          showCreateIcon: mode !== MODE.K8S,
          showDeleteIcon: mode === MODE.DOCKER,
          showEditorIcon: mode === MODE.DOCKER,
        }}
        title="All nodes"
      />
    </FullScreenDialog>
  );
};

NodeListDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.string,
  nodes: PropTypes.array,
  onClose: PropTypes.func,
};

NodeListDialog.defaultProps = {
  mode: MODE.K8S,
  nodes: [],
  onClose: () => {},
};

export default NodeListDialog;
