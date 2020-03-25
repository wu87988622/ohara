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

import React, { useState } from 'react';
import Typography from '@material-ui/core/Typography';
import AddIcon from '@material-ui/icons/Add';
import Link from '@material-ui/core/Link';
import styled from 'styled-components';
import { get, round, isEmpty, flatMap, filter, noop, capitalize } from 'lodash';

import { SelectTable } from 'components/common/Table';
import { FullScreenDialog } from 'components/common/Dialog';
import { Button } from 'components/common/Form';
import AddNodeDialog from './AddNodeDialog';
import ViewNodeDialog from './ViewNodeDialog';
import {
  useConfiguratorState,
  useListNodeDialog,
  useViewNodeDialog,
} from 'context';
import { QuickSearch } from 'components/common/Search';
import { MODE } from 'const';
import { NODE_STATE } from 'api/apiInterface/nodeInterface';
import * as hooks from 'hooks';

const Actions = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: ${props => props.theme.spacing(4)}px;

  .add-node-button {
    margin-left: auto;
  }
`;

const NodeDialog = () => {
  const { data: configuratorInfo } = useConfiguratorState();
  const nodes = hooks.useAllNodes();

  const {
    isOpen: isListNodeDialogOpen,
    close: closeListNodeDialog,
    data,
    setData,
  } = useListNodeDialog();

  const hasSelect = get(data, 'hasSelect', false);
  const hasSave = get(data, 'hasSave', false);
  const selected = get(data, 'selected', []);
  const blockedNodes = get(data, 'blockedNodes', []);
  const save = get(data, 'save', noop);

  const { open: openViewNodeDialog } = useViewNodeDialog();

  const [isAddNodeDialogOpen, setIsAddNodeDialogOpen] = useState(false);
  const [filteredNodes, setFilteredNodes] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  if (isListNodeDialogOpen && isLoading) {
    hooks.useFetchNodesAction()();
    setIsLoading(false);
  }

  const getResources = node => {
    const headers = getHeader();
    const allResources = headers.map(header => {
      const found = node.resources.find(
        resource => resource.name === header.id,
      );
      return found
        ? found
        : {
            name: header.id,
            used: null,
            value: null,
            unit: null,
          };
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
        .reduce((acc, cur) => acc.concat(cur), []);
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
        id: 'state',
        label: 'State',
      },
      {
        id: 'actions',
        label: 'Actions',
      },
    ];
    return headers;
  };

  const openDetailView = node => {
    openViewNodeDialog(node.hostname);
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
          state: capitalize(node.state),
          actions: viewButton(node),
        };
      });

  const blockedRows = rows.filter(row =>
    blockedNodes.map(node => node.hostname).includes(row.name),
  );

  const unavailableRows = rows.filter(
    row => row.state.toUpperCase() === NODE_STATE.unavailable,
  );

  return (
    <FullScreenDialog
      title="Nodes"
      open={isListNodeDialogOpen}
      handleClose={closeListNodeDialog}
      handleSave={() => {
        save(data);
        closeListNodeDialog();
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

          {configuratorInfo.mode === MODE.DOCKER && (
            <Button
              className="add-node-button"
              onClick={() => setIsAddNodeDialogOpen(true)}
              startIcon={<AddIcon />}
            >
              ADD NODE
            </Button>
          )}
        </Actions>

        <SelectTable
          headCells={tableHeaders()}
          rows={rows}
          title="All Nodes"
          hasSelect={hasSelect}
          selected={selected}
          setSelected={rows => setData({ ...data, selected: rows })}
          blockedRows={blockedRows}
          unavailableRows={unavailableRows}
        />

        <AddNodeDialog
          isOpen={isAddNodeDialogOpen}
          handleClose={() => setIsAddNodeDialogOpen(false)}
          mode={!isEmpty(configuratorInfo) ? configuratorInfo.mode : MODE.k8s}
        />

        <ViewNodeDialog
          mode={!isEmpty(configuratorInfo) ? configuratorInfo.mode : MODE.k8s}
        />
      </>
    </FullScreenDialog>
  );
};

export default NodeDialog;
