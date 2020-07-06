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
import _ from 'lodash';
import PropTypes from 'prop-types';

import { FullScreenDialog } from 'components/common/Dialog';
import { NodeTable } from 'components/Node';
import * as hooks from 'hooks';
import { KIND } from 'const';

const NodeListDialog = (props) => {
  const { isOpen, nodes, onClose } = props;

  const createNode = hooks.useCreateNodeAction();
  const deleteNode = hooks.useDeleteNodeAction();
  const updateNode = hooks.useUpdateNodeAction();
  const fetchNodes = hooks.useFetchNodesAction();

  const handleCreate = (nodeToCreate) => {
    return new Promise((resolve, reject) => {
      createNode(nodeToCreate, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const handleDelete = (nodeToDelete) => {
    return deleteNode(nodeToDelete?.hostname);
  };

  const handleUpdate = (nodeToUpdate) => {
    updateNode(nodeToUpdate);
  };

  const hasUsedServicesInNode = (node) =>
    !_(node.services)
      .filter((service) => service.name !== KIND.configurator)
      .flatMap((service) => service.clusterKeys)
      .isEmpty();

  return (
    <FullScreenDialog
      onClose={onClose}
      open={isOpen}
      testId="nodes-dialog"
      title="All nodes"
    >
      <NodeTable
        nodes={nodes}
        onCreate={handleCreate}
        onDelete={handleDelete}
        onUpdate={handleUpdate}
        options={{
          onRefreshIconClick: fetchNodes,
          disabledDeleteIcon: hasUsedServicesInNode,
          deleteTooltip: (node) => {
            return hasUsedServicesInNode(node)
              ? 'Cannot remove a node which has services running in it'
              : // use default toolTip otherwise
                undefined;
          },
          selection: false,
          showCreateIcon: true,
          showDeleteIcon: true,
          showEditorIcon: true,
          showRefreshIcon: true,
        }}
        title="All nodes"
      />
    </FullScreenDialog>
  );
};

NodeListDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  nodes: PropTypes.array,
  onClose: PropTypes.func,
};

NodeListDialog.defaultProps = {
  nodes: [],
  onClose: () => {},
};

export default NodeListDialog;
