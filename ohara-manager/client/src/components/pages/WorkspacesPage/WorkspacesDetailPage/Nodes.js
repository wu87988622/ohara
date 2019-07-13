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

import React, { useEffect, useCallback, useState } from 'react';
import toastr from 'toastr';
import PropTypes from 'prop-types';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';
import { get, isEmpty } from 'lodash';

import * as nodeApi from 'api/nodeApi';
import * as brokerApi from 'api/brokerApi';
import * as workerApi from 'api/workerApi';
import * as containerApi from 'api/containerApi';
import * as MESSAGES from 'constants/messages';
import * as commonUtils from 'utils/commonUtils';
import * as utils from './WorkspacesDetailPageUtils';
import Checkbox from '@material-ui/core/Checkbox';
import { Dialog } from 'components/common/Mui/Dialog';
import { SortTable } from 'components/common/Mui/Table';
import { Main, NewButton, StyledTable } from './styles';

const Nodes = props => {
  const { workspaceName } = props;
  const [useNodes, setUesNodes] = useState([]);
  const [broker, setBroker] = useState(null);
  const [unusedNodes, setUnusedNodes] = useState([]);
  const [selectNodes, setSelectNodes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [confirmDisabled, setConfirmDisabled] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [working, setWorking] = useState(false);

  const fetchWorker = useCallback(async () => {
    const workerRes = await workerApi.fetchWorker(workspaceName);
    const nodeRes = await nodeApi.fetchNodes();

    const workerNodes = get(workerRes, 'data.result.nodeNames', []);
    const nodes = get(nodeRes, 'data.result', []);
    const nodeNames = nodes.map(node => node.name);
    const workerNodeNames = nodeNames.filter(nodeName =>
      workerNodes.includes(nodeName),
    );

    setBroker(workerRes.data.result.brokerClusterName);

    const used = nodes.filter(node => workerNodeNames.includes(node.name));
    const unused = nodes.filter(node => !workerNodeNames.includes(node.name));

    setUesNodes(used);
    setUnusedNodes(unused);
    setLoading(false);
  }, [workspaceName]);

  useEffect(() => {
    fetchWorker();
  }, [fetchWorker]);

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

    const res = await containerApi.fetchContainers(name);
    const containers = get(res, 'data.result[0].containers', []);
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
    for (let selectNode of selectNodes) {
      await brokerApi.addNodeToBroker({
        name: broker,
        nodeName: selectNode,
      });
      await waitForServiceCreation({ name: broker });
      await workerApi.addNodeToWorker({
        name: workspaceName,
        nodeName: selectNode,
      });
      await waitForServiceCreation({ name: workspaceName });
    }
  };

  const handleAddNode = async () => {
    setWorking(true);
    if (selectNodes.length > 0) {
      await addNodeToService();
      await fetchWorker();
      toastr.success(MESSAGES.SERVICE_CREATION_SUCCESS);
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
          isLoading={loading}
          headRows={headRows}
          rows={rows}
          confirmDisabled={confirmDisabled}
          tableName="node"
        />
      </Main>

      <Dialog
        handelOpen={dialogOpen}
        handelClose={handleNodeSelectClose}
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
