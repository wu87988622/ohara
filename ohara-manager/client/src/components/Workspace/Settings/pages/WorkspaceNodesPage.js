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

import React, { useState, useRef } from 'react';
import { map, some, reject } from 'lodash';

import { NodeSelectorDialog, NodeTable } from 'components/Node';
import * as context from 'context';
import * as hooks from 'hooks';

function WorkspaceNodesPage() {
  const { data: configuratorInfo } = context.useConfiguratorState();
  const allNodes = hooks.useAllNodes();
  const nodesInWorkspace = hooks.useNodesInWorkspace();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();
  const workspace = hooks.useWorkspace();
  const [isSelectorDialogOpen, setIsSelectorDialogOpen] = useState(false);
  const selectorDialogRef = useRef(null);

  const handleAddIconClick = () => {
    setIsSelectorDialogOpen(true);
  };

  const handleRemove = nodeToRemove => {
    const shouldBeRemoved = some(
      nodesInWorkspace,
      n => n.hostname === nodeToRemove?.hostname,
    );

    if (shouldBeRemoved) {
      const newNodes = reject(
        nodesInWorkspace,
        n => n.hostname === nodeToRemove.hostname,
      );
      updateWorkspace({
        ...workspace,
        nodeNames: map(newNodes, n => n.hostname),
      });
      selectorDialogRef.current.setSelectedNodes(newNodes);
    }
  };

  const handleSelectorDialogConfirm = selectedNodes => {
    updateWorkspace({
      ...workspace,
      nodeNames: map(selectedNodes, n => n.hostname),
    });
    setIsSelectorDialogOpen(false);
  };

  return (
    <>
      <NodeTable
        nodes={nodesInWorkspace}
        onRemove={handleRemove}
        options={{
          mode: configuratorInfo?.mode,
          onAddIconClick: handleAddIconClick,
          showAddIcon: true,
          showCreateIcon: false,
          showDeleteIcon: false,
          showEditorIcon: false,
          showRemoveIcon: true,
        }}
        title="Workspace nodes"
      />

      <NodeSelectorDialog
        isOpen={isSelectorDialogOpen}
        nodes={allNodes}
        onClose={() => setIsSelectorDialogOpen(false)}
        onConfirm={handleSelectorDialogConfirm}
        ref={selectorDialogRef}
        selectedNodes={nodesInWorkspace}
        tableTitle="All nodes"
      />
    </>
  );
}

export default WorkspaceNodesPage;
