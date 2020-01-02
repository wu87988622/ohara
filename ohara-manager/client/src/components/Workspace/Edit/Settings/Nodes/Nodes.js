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
import { get, map } from 'lodash';

import Grid from '@material-ui/core/Grid';
import {
  useEditWorkspace,
  useWorkspace,
  useListNodeDialog,
  useNodeState,
} from 'context';
import NodeCard from './NodeCard';
import AddNodeCard from './AddNodeCard';

const Nodes = () => {
  const { stagingNodes } = useEditWorkspace();
  const { currentWorkspace } = useWorkspace();
  const { open: openListNodeDialog } = useListNodeDialog();
  const { data: allNodes } = useNodeState();
  const [dialogData, setDialogData] = React.useState({});

  const selectedNodes = get(
    dialogData,
    'selected',
    allNodes
      .filter(node =>
        currentWorkspace.settings.nodeNames.includes(node.hostname),
      )
      .map(node => {
        return {
          name: node.hostname,
        };
      }),
  );

  const handleClick = () => {
    const data = {
      selected: selectedNodes,
      ...dialogData,
      hasSelect: true,
      hasSave: true,
      blockedNodes: stagingNodes,
      save: setDialogData,
    };
    setDialogData(data);
    openListNodeDialog(data);
  };

  return (
    <>
      <Grid container spacing={3}>
        {AddNodeCard({
          onClick: handleClick,
          title: 'Add nodes',
          content: 'Click here to add nodes',
          sm: true,
        })}
        {map(selectedNodes, selectedNode => (
          <Grid item xs={6} key={selectedNode.name}>
            <NodeCard
              node={allNodes.find(node => node.hostname === selectedNode.name)}
            />
          </Grid>
        ))}
      </Grid>
    </>
  );
};

export default Nodes;
