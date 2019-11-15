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
import { round } from 'lodash';

import * as nodeApi from 'api/nodeApi';
import AddNodeDialog from './AddNodeDialog';
import { SelectTable } from 'components/common/Table';
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
    type,
    hasSelect,
    hasSave,
    selected,
    setSelected,
    setHasSelect,
  } = useNodeDialog();

  const [isAddNodeDialogOpen, setIsAddNodeDialogOpen] = useState(false);
  const [nodes, setNodes] = useState([]);

  const getRsources = node => {
    return node.resources.reduce(
      (obj, item) =>
        Object.assign(obj, {
          [item.name]: `${round(item.value, 1)} ${item.unit}|${round(
            item.used * 100,
            1,
          )}`,
        }),
      {},
    );
  };

  const getHeader = () => {
    return nodes.length > 0
      ? nodes[0].resources.map(res => {
          return {
            id: res.name,
            label: res.name,
          };
        })
      : [];
  };

  const tableHeaders = () => {
    let headers = [
      {
        id: 'name',
        label: 'Name',
      },
      ...getHeader(),
      {
        id: 'services',
        label: 'Services',
      },
      {
        id: 'actions',
        label: 'Actions',
      },
    ];
    return headers;
  };

  const viewButton = () => {
    return <Button variant="outlined">{'VIEW'}</Button>;
  };

  const rows = nodes.map(node => {
    return {
      name: node.hostname,
      ...getRsources(node),
      services: node.services.length,
      actions: viewButton(),
    };
  });

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
      handleClose={() => {
        setSelected([]);
        setHasSelect(false);
        setIsNodeDialogOpen(false);
      }}
      handleSave={() => {
        setIsNodeDialogOpen(false);
      }}
      hasSave={hasSave}
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

        <SelectTable
          headCells={tableHeaders()}
          rows={rows}
          title="All Nodes"
          hasSelect={hasSelect}
          selected={selected}
          setSelected={setSelected}
        />

        <AddNodeDialog
          isOpen={isAddNodeDialogOpen}
          handleClose={() => setIsAddNodeDialogOpen(false)}
          fetchNodes={fetchNodes}
          mode={type}
        />
      </>
    </FullScreenDialog>
  );
};

export default NodeDialog;
