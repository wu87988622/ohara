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

import React, { useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { filter, includes, map, reject } from 'lodash';
import Grid from '@material-ui/core/Grid';
import FormHelperText from '@material-ui/core/FormHelperText';

import * as context from 'context';
import * as hooks from 'hooks';
import { MODE } from 'const';
import Card from 'components/Workspace/Card/WorkspaceCard';
import SelectCard from 'components/Workspace/Card/SelectCard';
import { NodeSelectorDialog } from 'components/Node';

const renderFromHelper = ({ touched, error }) => {
  if (!(touched && error)) {
    return;
  } else {
    return <FormHelperText error>{touched && error}</FormHelperText>;
  }
};

const NodesField = props => {
  const {
    input: { onBlur, onFocus, onChange, value },
    meta: { touched, error },
  } = props;

  const { data: configuratorInfo } = context.useConfiguratorState();
  const mode = configuratorInfo?.mode;
  const allNodes = hooks.useAllNodes();
  const createNode = hooks.useCreateNodeAction();

  const [selectedNodes, setSelectedNodes] = useState(() => {
    // value is an array of hostname, like ['dev01', 'dev02'].
    return filter(allNodes, node => includes(value, node.hostname));
  });
  const [isSelectorDialogOpen, setIsSelectorDialogOpen] = useState(false);
  const selectorDialogRef = useRef(null);

  const openSelectorDialog = () => setIsSelectorDialogOpen(true);
  const closeSelectorDialog = () => setIsSelectorDialogOpen(false);

  const deleteNode = nodeToDelete => () => {
    const remaining = reject(
      selectedNodes,
      selectedNode => selectedNode.hostname === nodeToDelete?.hostname,
    );
    const newSelectedNodes = [...remaining];
    setSelectedNodes(newSelectedNodes);
    selectorDialogRef.current.setSelectedNodes(newSelectedNodes);
    onChange(newSelectedNodes.map(node => node.hostname));
  };

  const handleSelectorConfirm = (selectedNodes = []) => {
    onBlur();
    setSelectedNodes(selectedNodes);
    onChange(selectedNodes.map(node => node.hostname));
    closeSelectorDialog();
  };

  return (
    <>
      <Grid
        container
        direction="row"
        justify="flex-start"
        alignItems="flex-start"
      >
        {map(selectedNodes, node => {
          return (
            <Grid item xs={4} key={node?.hostname}>
              <SelectCard rows={node} handleClose={deleteNode(node)} />
            </Grid>
          );
        })}
        <Grid item xs={selectedNodes?.length > 0 ? 4 : 12} key="select_nodes">
          <Card
            onClick={() => {
              onFocus();
              openSelectorDialog();
            }}
            title="Select nodes"
            content="Click here to select nodes"
            sm={selectedNodes?.length > 0}
          />
        </Grid>
      </Grid>
      {renderFromHelper({ touched, error })}

      <NodeSelectorDialog
        isOpen={isSelectorDialogOpen}
        onClose={() => {
          onBlur();
          closeSelectorDialog();
        }}
        onConfirm={handleSelectorConfirm}
        ref={selectorDialogRef}
        tableProps={{
          nodes: allNodes,
          onCreate: createNode,
          options: {
            mode,
            selectedNodes,
            showCreateIcon: mode !== MODE.K8S,
          },
          title: 'All node',
        }}
        title="Select nodes"
      />
    </>
  );
};

NodesField.propTypes = {
  input: PropTypes.shape({
    name: PropTypes.string.isRequired,
    onBlur: PropTypes.func.isRequired,
    onChange: PropTypes.func.isRequired,
    onFocus: PropTypes.func.isRequired,
    value: PropTypes.arrayOf(PropTypes.string).isRequired,
  }).isRequired,
  meta: PropTypes.shape({
    error: PropTypes.string,
    invalid: PropTypes.bool,
    touched: PropTypes.bool,
  }),
};

export default NodesField;
