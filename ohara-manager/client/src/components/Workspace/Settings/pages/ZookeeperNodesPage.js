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

function ZookeeperNodesPage() {
  const { data: configuratorInfo } = context.useConfiguratorState();
  const workspace = hooks.useWorkspace();
  const zookeeper = hooks.useZookeeper();
  const nodesInZookeeper = hooks.useNodesInZookeeper();
  const nodesInWorkspace = hooks.useNodesInWorkspace();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();
  const selectorDialogRef = useRef(null);
  const [isSelectorDialogOpen, setIsSelectorDialogOpen] = useState(false);

  const documentation = useMemo(() => {
    return find(
      zookeeper?.settingDefinitions,
      (definition) => definition.key === 'nodeNames',
    )?.documentation;
  }, [zookeeper]);

  const zookeeperNodesInWorkspace = useMemo(() => {
    return workspace?.zookeeper?.nodeNames
      ? filter(
          map(workspace.zookeeper.nodeNames, (nodeName) =>
            find(nodesInWorkspace, (node) => node.hostname === nodeName),
          ),
        )
      : nodesInZookeeper;
  }, [workspace, nodesInWorkspace, nodesInZookeeper]);

  const handleAddIconClick = () => {
    setIsSelectorDialogOpen(true);
  };

  const handleUndoIconClick = (nodeClicked) => {
    const currentIndex = workspace?.zookeeper?.nodeNames?.indexOf(
      nodeClicked?.hostname,
    );
    const newNodeNames = [...workspace?.zookeeper?.nodeNames];

    if (currentIndex === -1) {
      newNodeNames.push(nodeClicked?.hostname);
    } else {
      newNodeNames.splice(currentIndex, 1);
    }
    updateWorkspace({
      ...workspace,
      zookeeper: {
        ...workspace?.zookeeper,
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
      zookeeperNodesInWorkspace,
      (n) => n.hostname === nodeToRemove?.hostname,
    );

    if (shouldBeRemoved) {
      const newNodes = reject(
        zookeeperNodesInWorkspace,
        (n) => n.hostname === nodeToRemove.hostname,
      );
      updateWorkspace({
        ...workspace,
        zookeeper: {
          ...workspace?.zookeeper,
          nodeNames: map(newNodes, (n) => n.hostname),
        },
      });
      selectorDialogRef.current.setSelectedNodes(newNodes);
    }
  };

  const handleSelectorDialogConfirm = (selectedNodes) => {
    updateWorkspace({
      ...workspace,
      zookeeper: {
        ...workspace?.zookeeper,
        nodeNames: map(selectedNodes, (n) => n.hostname),
      },
    });
    setIsSelectorDialogOpen(false);
  };

  return (
    <>
      <NodeTable
        nodes={zookeeperNodesInWorkspace}
        onDelete={handleDelete}
        options={{
          comparison: true,
          comparedNodes: nodesInZookeeper,
          disabledRemoveIcon: size(zookeeperNodesInWorkspace) <= 1,
          mode: configuratorInfo?.mode,
          onAddIconClick: handleAddIconClick,
          onUndoIconClick: handleUndoIconClick,
          prompt: documentation,
          removeTooltip:
            size(zookeeperNodesInWorkspace) <= 1
              ? 'Cannot remove node, because there must be at least one'
              : 'Remove node',
          showAddIcon: true,
          showCreateIcon: false,
          showDeleteIcon: false,
          showEditorIcon: false,
          showRemoveIcon: true,
        }}
        title="Zookeeper nodes"
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
            selectedNodes: zookeeperNodesInWorkspace,
          },
          title: 'Workspace nodes',
        }}
      />
    </>
  );
}

export default ZookeeperNodesPage;
