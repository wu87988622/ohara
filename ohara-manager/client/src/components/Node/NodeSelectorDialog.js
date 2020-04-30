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

import React, { useState, useImperativeHandle } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import { isEqual, sortedUniq } from 'lodash';
import Typography from '@material-ui/core/Typography';

import { Dialog } from 'components/common/Dialog';
import NodeTable from './NodeTable';

const Wrapper = styled.div(
  ({ theme }) => css`
    .prompt {
      margin: ${theme.spacing(1.5, 0)};
    }
  `,
);

const NodeSelectorDialog = React.forwardRef((props, ref) => {
  const { isOpen, onClose, onConfirm, prompt, tableProps, title } = props;

  const [selectedNodes, setSelectedNodes] = useState(
    tableProps?.options?.selectedNodes,
  );

  const saveable = isEqual(
    sortedUniq(selectedNodes),
    sortedUniq(tableProps?.options?.selectedNodes),
  );

  const handleSelectionChange = selectNodes => {
    setSelectedNodes(selectNodes);
  };

  const handleCancel = () => {
    setSelectedNodes(tableProps?.options?.selectedNodes);
    onClose();
  };

  const handleConfirm = () => {
    onConfirm(selectedNodes);
  };

  useImperativeHandle(ref, () => ({
    setSelectedNodes,
  }));

  return (
    <Dialog
      title={title}
      open={isOpen}
      onClose={handleCancel}
      onConfirm={handleConfirm}
      confirmDisabled={saveable}
      confirmText="Save"
      maxWidth="md"
    >
      <Wrapper>
        {prompt && (
          <Typography className="prompt" variant="h6">
            {prompt}
          </Typography>
        )}
        <NodeTable
          {...tableProps}
          onSelectionChange={handleSelectionChange}
          options={{
            ...tableProps?.options,
            selection: true,
            showCreateIcon: false,
            showDeleteIcon: false,
            showEditorIcon: false,
          }}
        />
      </Wrapper>
    </Dialog>
  );
});

NodeSelectorDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
  onConfirm: PropTypes.func,
  prompt: PropTypes.string,
  tableProps: PropTypes.shape({
    options: PropTypes.shape({
      selectedNodes: PropTypes.array,
    }),
  }),
  title: PropTypes.string,
};

NodeSelectorDialog.defaultProps = {
  onClose: () => {},
  onConfirm: () => {},
  prompt: null,
  tableProps: {},
  title: 'Select node',
};

export default NodeSelectorDialog;
