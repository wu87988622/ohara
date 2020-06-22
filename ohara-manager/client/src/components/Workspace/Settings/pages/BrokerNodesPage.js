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

import React, { useMemo, useState, useRef } from 'react';
import { filter, find, map, size, some, reject } from 'lodash';

import { NodeSelectorDialog, NodeTable } from 'components/Node';
import * as context from 'context';
import * as hooks from 'hooks';

function BrokerNodesPage() {
  const { data: configuratorInfo } = context.useConfiguratorState();
  const workspace = hooks.useWorkspace();
  const broker = hooks.useBroker();
  const nodesInBroker = hooks.useNodesInBroker();
  const nodesInWorkspace = hooks.useNodesInWorkspace();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();
  const selectorDialogRef = useRef(null);
  const [isSelectorDialogOpen, setIsSelectorDialogOpen] = useState(false);

  const documentation = useMemo(() => {
    return find(
      broker?.settingDefinitions,
      (definition) => definition.key === 'nodeNames',
    )?.documentation;
  }, [broker]);

  const brokerNodesInWorkspace = useMemo(() => {
    return workspace?.broker?.nodeNames
      ? filter(
          map(workspace.broker.nodeNames, (nodeName) =>
            find(nodesInWorkspace, (node) => node.hostname === nodeName),
          ),
        )
      : nodesInBroker;
  }, [workspace, nodesInWorkspace, nodesInBroker]);

  const handleAddIconClick = () => {
    setIsSelectorDialogOpen(true);
  };

  const handleUndoIconClick = (nodeClicked) => {
    const currentIndex = workspace?.broker?.nodeNames?.indexOf(
      nodeClicked?.hostname,
    );
    const newNodeNames = [...workspace?.broker?.nodeNames];

    if (currentIndex === -1) {
      newNodeNames.push(nodeClicked?.hostname);
    } else {
      newNodeNames.splice(currentIndex, 1);
    }
    updateWorkspace({
      ...workspace,
      broker: {
        ...workspace?.broker,
        nodeNames: newNodeNames,
      },
    });
    selectorDialogRef.current.setSelectedNodes(
      map(newNodeNames, (nodeName) =>
        find(nodesInWorkspace, (n) => n.hostname === nodeName),
      ),
    );
  };

  const handleDelete = (nodeToRemove) => {
    const shouldBeRemoved = some(
      brokerNodesInWorkspace,
      (n) => n.hostname === nodeToRemove?.hostname,
    );

    if (shouldBeRemoved) {
      const newNodes = reject(
        brokerNodesInWorkspace,
        (n) => n.hostname === nodeToRemove.hostname,
      );
      updateWorkspace({
        ...workspace,
        broker: {
          ...workspace?.broker,
          nodeNames: map(newNodes, (n) => n.hostname),
        },
      });
      selectorDialogRef.current.setSelectedNodes(newNodes);
    }
  };

  const handleSelectorDialogConfirm = (selectedNodes) => {
    updateWorkspace({
      ...workspace,
      broker: {
        ...workspace?.broker,
        nodeNames: map(selectedNodes, (n) => n.hostname),
      },
    });
    setIsSelectorDialogOpen(false);
  };

  return (
    <>
      <NodeTable
        nodes={brokerNodesInWorkspace}
        onDelete={handleDelete}
        options={{
          comparison: true,
          comparedNodes: nodesInBroker,
          disabledRemoveIcon: size(brokerNodesInWorkspace) <= 1,
          mode: configuratorInfo?.mode,
          onAddIconClick: handleAddIconClick,
          onUndoIconClick: handleUndoIconClick,
          prompt: documentation,
          removeTooltip:
            size(brokerNodesInWorkspace) <= 1
              ? 'Cannot remove node, because there must be at least one'
              : 'Remove node',
          showAddIcon: true,
          showCreateIcon: false,
          showDeleteIcon: false,
          showEditorIcon: false,
          showRemoveIcon: true,
        }}
        title="Broker nodes"
      />

      <NodeSelectorDialog
        isOpen={isSelectorDialogOpen}
        onClose={() => setIsSelectorDialogOpen(false)}
        onConfirm={handleSelectorDialogConfirm}
        ref={selectorDialogRef}
        tableProps={{
          nodes: nodesInWorkspace,
          options: {
            prompt:
              'If you want to have more selectable nodes, please go to the nodes in the workspace to add new nodes.',
            selectedNodes: brokerNodesInWorkspace,
          },
          title: 'Workspace nodes',
        }}
      />
    </>
  );
}

export default BrokerNodesPage;
