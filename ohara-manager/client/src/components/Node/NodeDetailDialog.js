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
import PropTypes from 'prop-types';

import Grid from '@material-ui/core/Grid';
import Dialog from 'components/common/Dialog/Dialog';
import NodeInfoTable from './NodeInfoTable';
import NodeServiceTable from './NodeServiceTable';

function NodeDetailDialog({ mode, node, isOpen, onClose }) {
  return (
    <Dialog
      open={isOpen}
      onClose={onClose}
      title="View node detail"
      maxWidth="md"
      showActions={false}
    >
      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          {node && <NodeInfoTable mode={mode} node={node} />}
        </Grid>
        <Grid item xs={12} md={8}>
          {node && <NodeServiceTable node={node} />}
        </Grid>
      </Grid>
    </Dialog>
  );
}

NodeDetailDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.string,
  node: PropTypes.object,
  onClose: PropTypes.func,
};

NodeDetailDialog.defaultProps = {
  onClose: () => {},
};

export default NodeDetailDialog;
