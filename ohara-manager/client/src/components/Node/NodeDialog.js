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

import React, { useEffect, useState, useCallback } from 'react';
import Typography from '@material-ui/core/Typography';
import AddIcon from '@material-ui/icons/Add';
import styled from 'styled-components';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';

import * as nodeApi from 'api/nodeApi';
import AddNodeDialog from './AddNodeDialog';
import { Table } from 'components/common/Table';
import { FullScreenDialog } from 'components/common/Dialog';
import { useNodeDialog } from 'context/NodeDialogContext';
import { Button } from 'components/common/Form';

const Actions = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: ${props => props.theme.spacing(4)}px;

  button {
    margin-left: auto;
  }
`;

const NodeDialog = () => {
  const {
    isOpen: isNodeDialogOpen,
    setIsOpen: setIsNodeDialogOpen,
  } = useNodeDialog();

  const [isAddNodeDialogOpen, setIsAddNodeDialogOpen] = useState(false);
  const [nodes, setNodes] = useState([]);

  const tableHeaders = [
    'Name',
    'Cores',
    'CPU',
    'Memory',
    'Disks',
    'Services',
    'Actions',
  ];

  const fetchNodes = useCallback(async () => {
    const data = await nodeApi.getAll();
    setNodes(data);
  }, []);

  useEffect(() => {
    fetchNodes();
  }, [fetchNodes]);

  return (
    <FullScreenDialog
      title="Nodes"
      open={isNodeDialogOpen}
      handleClose={() => setIsNodeDialogOpen(false)}
    >
      <>
        <Actions>
          <Typography>Quick filter here</Typography>
          <Button
            onClick={() => setIsAddNodeDialogOpen(true)}
            startIcon={<AddIcon />}
          >
            ADD NODE
          </Button>
        </Actions>

        <Table headers={tableHeaders} title="All Nodes">
          {nodes.map(node => (
            <TableRow key={node.hostname}>
              <TableCell>{node.hostname}</TableCell>
              <TableCell></TableCell>
              <TableCell></TableCell>
              <TableCell></TableCell>
              <TableCell></TableCell>
              <TableCell>{node.services.length}</TableCell>
              <TableCell align="right">
                <Button variant="outlined" color="primary">
                  View
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </Table>

        <AddNodeDialog
          isOpen={isAddNodeDialogOpen}
          handleClose={() => setIsAddNodeDialogOpen(false)}
          fetchNodes={fetchNodes}
        />
      </>
    </FullScreenDialog>
  );
};

export default NodeDialog;
