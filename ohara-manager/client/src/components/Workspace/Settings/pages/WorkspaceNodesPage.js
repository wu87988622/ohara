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
import { capitalize, includes, join, map, reject, some, toUpper } from 'lodash';

import { NodeSelectorDialog, NodeTable } from 'components/Node';
import * as context from 'context';
import * as hooks from 'hooks';
import { MODE } from 'const';

const BROKER = 'broker';
const STREAM = 'stream';
const WORKER = 'worker';
const ZOOKEEPER = 'zookeeper';

function WorkspaceNodesPage() {
  const { data: configuratorInfo } = context.useConfiguratorState();
  const allNodes = hooks.useAllNodes();
  const nodesInWorkspace = hooks.useNodesInWorkspace();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();
  const fetchNodes = hooks.useFetchNodesAction();
  const workspace = hooks.useWorkspace();
  const [isSelectorDialogOpen, setIsSelectorDialogOpen] = useState(false);
  const selectorDialogRef = useRef(null);

  const broker = hooks.useBroker();
  const streams = hooks.useStreams();
  const worker = hooks.useWorker();
  const zookeeper = hooks.useZookeeper();

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

  const isUsedByBroker = node => {
    return (
      includes(broker?.nodeNames, node?.hostname) ||
      includes(workspace?.broker?.nodeNames, node?.hostname)
    );
  };

  const isUsedByStream = node => {
    return some(streams, stream => includes(stream?.nodeNames, node?.hostname));
  };

  const isUsedByWorker = node => {
    return (
      includes(worker?.nodeNames, node?.hostname) ||
      includes(workspace?.worker?.nodeNames, node?.hostname)
    );
  };

  const isUsedByZookeeper = node => {
    return (
      includes(zookeeper?.nodeNames, node?.hostname) ||
      includes(workspace?.zookeeper?.nodeNames, node?.hostname)
    );
  };

  return (
    <>
      <NodeTable
        nodes={nodesInWorkspace}
        onRemove={handleRemove}
        options={{
          customColumns: [
            {
              title: 'Used',
              customFilterAndSearch: (filterValue, node) => {
                const value = [];
                if (isUsedByBroker(node)) value.push(BROKER);
                if (isUsedByStream(node)) value.push(STREAM);
                if (isUsedByWorker(node)) value.push(WORKER);
                if (isUsedByZookeeper(node)) value.push(ZOOKEEPER);
                return includes(toUpper(join(value)), toUpper(filterValue));
              },
              render: node => {
                return (
                  <>
                    {isUsedByBroker(node) && <div>{capitalize(BROKER)}</div>}
                    {isUsedByStream(node) && <div>{capitalize(STREAM)}</div>}
                    {isUsedByWorker(node) && <div>{capitalize(WORKER)}</div>}
                    {isUsedByZookeeper(node) && (
                      <div>{capitalize(ZOOKEEPER)}</div>
                    )}
                  </>
                );
              },
            },
          ],
          disabledRemoveIcon: node => {
            return (
              isUsedByBroker(node) ||
              isUsedByStream(node) ||
              isUsedByWorker(node) ||
              isUsedByZookeeper(node)
            );
          },
          mode: configuratorInfo?.mode,
          onAddIconClick: handleAddIconClick,
          showAddIcon: true,
          showCreateIcon: false,
          showDeleteIcon: false,
          showEditorIcon: false,
          showRemoveIcon: true,
          showServicesColumn: false,
        }}
        title="Workspace nodes"
      />

      <NodeSelectorDialog
        isOpen={isSelectorDialogOpen}
        onClose={() => setIsSelectorDialogOpen(false)}
        onConfirm={handleSelectorDialogConfirm}
        ref={selectorDialogRef}
        tableProps={{
          nodes: allNodes,
          options: {
            onRefreshIconClick: fetchNodes,
            selectedNodes: nodesInWorkspace,
            showRefreshIcon: configuratorInfo?.mode === MODE.K8S,
          },
          title: 'All nodes',
        }}
      />
    </>
  );
}

export default WorkspaceNodesPage;
