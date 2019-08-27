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
import PropTypes from 'prop-types';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';
import { get, isEmpty } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as commonUtils from 'utils/commonUtils';
import * as utils from '../WorkspacesDetailPageUtils';
import Checkbox from '@material-ui/core/Checkbox';
import { Dialog } from 'components/common/Mui/Dialog';
import { SortTable } from 'components/common/Mui/Table';
import { Main, NewButton, StyledTable } from './styles';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';
import useSnackbar from 'components/context/Snackbar/useSnackbar';

const Nodes = props => {
  const { workspaceName } = props;
  const [selectNodes, setSelectNodes] = useState([]);
  const [confirmDisabled, setConfirmDisabled] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [working, setWorking] = useState(false);
  const { putApi: putWorker } = useApi.usePutApi(URL.WORKER_URL);
  const { putApi: putBroker } = useApi.usePutApi(URL.BROKER_URL);
  const { getData, getApi } = useApi.useGetApi(URL.CONTAINER_URL);
  const { showMessage } = useSnackbar();

  const { data: workerRes, isLoading, refetch } = useApi.useFetchApi(
    `${URL.WORKER_URL}/${workspaceName}`,
  );
  const { data: nodeRes } = useApi.useFetchApi(URL.NODE_URL);
  const workerNodes = get(workerRes, 'data.result.nodeNames', []);
  const broker = get(workerRes, 'data.result.brokerClusterName', null);
  const nodes = get(nodeRes, 'data.result', []);
  const nodeNames = nodes.map(node => node.name);
  const workerNodeNames = nodeNames.filter(nodeName =>
    workerNodes.includes(nodeName),
  );

  const useNodes = nodes.filter(node => workerNodeNames.includes(node.name));
  const unusedNodes = nodes.filter(
    node => !workerNodeNames.includes(node.name),
  );

  const headRows = [
    { id: 'name', label: 'Node name' },
    { id: 'port', label: 'Port' },
    { id: 'lastModified', label: 'Last modified' },
  ];

  const rows = useNodes.map(({ name, port, lastModified }) => {
    return {
      name,
      port,
      lastModified: utils.getDateFromTimestamp(lastModified),
    };
  });

  const handleNodeSelectClose = () => {
    setDialogOpen(false);
  };

  const handleNodeSelect = event => {
    const { id, checked } = event.target;
    if (checked) {
      if (!selectNodes.some(node => node === id)) {
        selectNodes.push(id);
        setSelectNodes(selectNodes);
        setConfirmDisabled(false);
      }
    } else {
      if (selectNodes.some(node => node === id)) {
        const index = selectNodes.indexOf(id);
        selectNodes.splice(index, 1);
        setSelectNodes(selectNodes);
      }
      if (isEmpty(selectNodes.length)) {
        setConfirmDisabled(true);
      }
    }
  };

  const waitForServiceCreation = async params => {
    const { retryCount = 0, name } = params;

    if (retryCount > 5) return;

    await getApi(name);
    const containers = get(getData(), 'data.result[0].containers', []);
    const nodeNames = containers.map(container => {
      return container.state === 'RUNNING' ? container.nodeName : '';
    });

    if (nodeNames.some(n => selectNodes.includes(n))) {
      return;
    }
    await commonUtils.sleep(2000);
    await waitForServiceCreation({ retryCount: retryCount + 1, name });
  };

  const addNodeToService = async () => {
    // A bug from the eslint, already fixed but not updated to CRA yet.
    // https://github.com/babel/babel-eslint/issues/791

    // eslint-disable-next-line
    for (let selectNode of selectNodes) {
      await putBroker(`/${broker}/${selectNode}`);
      await waitForServiceCreation({ name: broker });
      await putWorker(`/${workspaceName}/${selectNode}`);
      await waitForServiceCreation({ name: workspaceName });
    }
  };

  const handleAddNode = async () => {
    setWorking(true);
    if (selectNodes.length > 0) {
      await addNodeToService();
      refetch();
      showMessage(MESSAGES.SERVICE_CREATION_SUCCESS);
      setSelectNodes([]);
    }
    setWorking(false);
    setDialogOpen(false);
  };

  const handelOpen = () => {
    if (isEmpty(selectNodes.length)) {
      setConfirmDisabled(true);
    } else {
      setConfirmDisabled(false);
    }
    setDialogOpen(true);
  };

  const headers = ['Select', 'Node name', 'Port'];

  return (
    <>
      <NewButton text="New node" onClick={handelOpen} />
      <Main>
        <SortTable
          isLoading={isLoading}
          headRows={headRows}
          rows={rows}
          confirmDisabled={confirmDisabled}
          tableName="node"
        />
      </Main>

      <Dialog
        open={dialogOpen}
        handleClose={handleNodeSelectClose}
        title="Add Node"
        handleConfirm={handleAddNode}
        confirmDisabled={confirmDisabled}
        loading={working}
        testId="node-new-dialog"
      >
        <StyledTable headers={headers}>
          <>
            {unusedNodes.map(node => {
              const { name, port } = node;
              return (
                <TableRow key={name}>
                  <TableCell>
                    <Checkbox
                      id={name}
                      color="primary"
                      onChange={handleNodeSelect}
                    />
                  </TableCell>
                  <TableCell>{name}</TableCell>
                  <TableCell align="right">{port}</TableCell>
                </TableRow>
              );
            })}

            {useNodes.map(node => {
              const { name, port } = node;
              return (
                <TableRow key={name} selected>
                  <TableCell>
                    <Checkbox
                      id={name}
                      color="primary"
                      onChange={handleNodeSelect}
                      checked
                      disabled
                    />
                  </TableCell>
                  <TableCell>{name}</TableCell>
                  <TableCell align="right">{port}</TableCell>
                </TableRow>
              );
            })}
          </>
        </StyledTable>
      </Dialog>
    </>
  );
};

Nodes.propTypes = {
  workspaceName: PropTypes.string.isRequired,
};

export default Nodes;
