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

import React, { useState, useEffect } from 'react';
import Typography from '@material-ui/core/Typography';
import AddIcon from '@material-ui/icons/Add';
import Link from '@material-ui/core/Link';
import styled from 'styled-components';
import { round, isEmpty, flatMap, filter } from 'lodash';

import AddNodeDialog from './AddNodeDialog';
import { SelectTable } from 'components/common/Table';
import { FullScreenDialog } from 'components/common/Dialog';
import { useNodeDialog } from 'context/NodeDialogContext';
import { Button } from 'components/common/Form';
import ViewNodeDialog from './ViewNodeDialog';
import { useConfiguratorState, useNodeState, useViewNodeDialog } from 'context';
import { QuickSearch } from 'components/common/Search';

const Actions = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: ${props => props.theme.spacing(4)}px;

  button {
    margin-left: auto;
  }
`;

const NodeDialog = () => {
  const { data: configuratorInfo } = useConfiguratorState();
  const {
    isOpen: isNodeDialogOpen,
    setIsOpen: setIsNodeDialogOpen,
    type,
    setType,
    hasSelect,
    hasSave,
    selected,
    setSelected,
    setHasSelect,
  } = useNodeDialog();
  const { open: openViewNodeDialog } = useViewNodeDialog();

  const { data: nodes } = useNodeState();

  const [isAddNodeDialogOpen, setIsAddNodeDialogOpen] = useState(false);
  const [filteredNodes, setFilteredNodes] = useState([]);

  useEffect(() => {
    if (isEmpty(configuratorInfo) || !configuratorInfo) return;
    setType(configuratorInfo.mode);
  }, [setType, configuratorInfo]);

  const getResources = node => {
    const headers = getHeader();
    const allResources = headers.map(header => {
      const found = node.resources.find(
        resource => resource.name === header.id,
      );
      return found ? found : { name: header };
    });
    return allResources.reduce(
      (obj, item) =>
        Object.assign(obj, {
          [item.name]: `${round(item.value, 1)} ${item.unit}${
            item.used ? `|${round(item.used * 100, 1)}` : ''
          }`,
        }),
      {},
    );
  };

  const getHeader = () => {
    if (filteredNodes.length > 0) {
      const headers = filteredNodes
        .map(node => node.resources.map(resource => resource.name))
        .flat(1);
      const distinctHeaders = [...new Set(headers)];
      return distinctHeaders.map(header => {
        return { id: header, label: header };
      });
    } else {
      return [];
    }
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

  const openDetailView = node => {
    openViewNodeDialog(node);
  };

  const viewButton = node => {
    return (
      <Button variant="outlined" onClick={() => openDetailView(node)}>
        {'VIEW'}
      </Button>
    );
  };

  const rows = isEmpty(filteredNodes)
    ? []
    : filteredNodes.map(node => {
        return {
          name: node.hostname,
          ...getResources(node),
          services: (
            <Typography>
              <Link
                component="button"
                herf="#"
                onClick={() => openDetailView(node)}
              >
                {
                  flatMap(
                    filter(
                      node.services,
                      service => service.name !== 'configurator',
                    ),
                    service => service.clusterKeys,
                  ).length
                }
              </Link>
            </Typography>
          ),
          actions: viewButton(node),
        };
      });

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
          <Typography component="div">
            <QuickSearch
              data={nodes}
              keys={['hostname']}
              placeholder="Quick Filter"
              setResults={setFilteredNodes}
            />
          </Typography>
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
          mode={type}
        />

        <ViewNodeDialog mode={type} />
      </>
    </FullScreenDialog>
  );
};

export default NodeDialog;
