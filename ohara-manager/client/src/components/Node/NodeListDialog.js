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

import { FullScreenDialog } from 'components/common/Dialog';
import { MODE } from 'const';
import NodeCreateDialog from './NodeCreateDialog';
import NodeDeleteDialog from './NodeDeleteDialog';
import NodeDetailDialog from './NodeDetailDialog';
import NodeEditorDialog from './NodeEditorDialog';
import NodeTable from './NodeTable';

const NodeListDialog = props => {
  const { dialogTitle, isOpen, mode, nodes, onClose, tableTitle } = props;

  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isDetailDialogOpen, setIsDetailDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [isEditorDialogOpen, setIsEditorDialogOpen] = useState(false);
  const [activeNode, setActiveNode] = useState();

  const handleCreateClick = () => {
    setIsCreateDialogOpen(true);
  };

  const handleDeleteClick = node => {
    setIsDeleteDialogOpen(true);
    setActiveNode(node);
  };

  const handleDetailClick = node => {
    setIsDetailDialogOpen(true);
    setActiveNode(node);
  };

  const handleEditorClick = node => {
    setIsEditorDialogOpen(true);
    setActiveNode(node);
  };

  return (
    <FullScreenDialog title={dialogTitle} open={isOpen} handleClose={onClose}>
      <NodeTable
        mode={mode}
        nodes={nodes}
        onCreateClick={handleCreateClick}
        onDetailClick={handleDetailClick}
        onDeleteClick={handleDeleteClick}
        onEditorClick={handleEditorClick}
        title={tableTitle}
        showCreateIcon={mode !== MODE.K8S}
        showDeleteIcon={mode === MODE.DOCKER}
        showEditorIcon={mode === MODE.DOCKER}
      />
      <NodeCreateDialog
        isOpen={isCreateDialogOpen}
        mode={mode}
        onClose={() => setIsCreateDialogOpen(false)}
      />
      <NodeDeleteDialog
        isOpen={isDeleteDialogOpen}
        node={activeNode}
        onClose={() => setIsDeleteDialogOpen(false)}
      />
      <NodeDetailDialog
        isOpen={isDetailDialogOpen}
        mode={mode}
        node={activeNode}
        onClose={() => setIsDetailDialogOpen(false)}
      />
      <NodeEditorDialog
        isOpen={isEditorDialogOpen}
        node={activeNode}
        onClose={() => setIsEditorDialogOpen(false)}
      />
    </FullScreenDialog>
  );
};

NodeListDialog.propTypes = {
  dialogTitle: PropTypes.string,
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.string,
  nodes: PropTypes.array,
  onClose: PropTypes.func,
  tableTitle: PropTypes.string,
};

NodeListDialog.defaultProps = {
  dialogTitle: 'Nodes',
  mode: MODE.K8S,
  nodes: [],
  onClose: () => {},
  tableTitle: 'Nodes',
};

export default NodeListDialog;
