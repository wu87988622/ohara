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
import { find, map, pull } from 'lodash';
import Grid from '@material-ui/core/Grid';
import FormHelperText from '@material-ui/core/FormHelperText';

import * as context from 'context';
import Card from 'components/Workspace/Card/WorkspaceCard';
import SelectCard from 'components/Workspace/Card/SelectCard';
import SelectNodeDialog from 'components/Node/SelectNodeDialog';

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

  const { data: allNodes } = context.useNodeState();

  // nodeNames is an array of hostname, like ['dev01', 'dev02'].
  const [nodeNames, setNodeNames] = useState([...value]);
  const [isSelectNodeDialogOpen, setIsSelectNodeDialogOpen] = useState(false);
  const openSelectNodeDialog = () => setIsSelectNodeDialogOpen(true);
  const closeSelectNodeDialog = () => setIsSelectNodeDialogOpen(false);

  const selectNodeDialogRef = useRef(null);

  const handleDelete = nodeName => () => {
    const remaining = pull(nodeNames, nodeName);
    const newNodeNames = [...remaining];
    setNodeNames(newNodeNames);
    onChange(newNodeNames);
  };

  const handleSelect = (selected = []) => {
    onBlur();
    setNodeNames(selected);
    onChange(selected);
    closeSelectNodeDialog();
  };

  return (
    <>
      <Grid
        container
        direction="row"
        justify="flex-start"
        alignItems="flex-start"
      >
        {map(nodeNames, nodeName => {
          const node = find(allNodes, n => n.hostname === nodeName);
          if (node) {
            return (
              <Grid item xs={4}>
                <SelectCard rows={node} handleClose={handleDelete(nodeName)} />
              </Grid>
            );
          }
        })}
        <Grid item xs={nodeNames.length > 0 ? 4 : 12}>
          <Card
            onClick={() => {
              onFocus();
              openSelectNodeDialog();
            }}
            title="Select nodes"
            content="Click here to select nodes"
            sm={nodeNames.length > 0}
          />
        </Grid>
      </Grid>
      {renderFromHelper({ touched, error })}
      <SelectNodeDialog
        initialValues={nodeNames}
        isOpen={isSelectNodeDialogOpen}
        onClose={() => {
          onBlur();
          closeSelectNodeDialog();
        }}
        onSubmit={handleSelect}
        ref={selectNodeDialogRef}
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
    value: PropTypes.string.isRequired,
  }).isRequired,
  meta: PropTypes.shape({
    error: PropTypes.string,
    invalid: PropTypes.bool,
    touched: PropTypes.bool,
  }),
};

export default NodesField;
