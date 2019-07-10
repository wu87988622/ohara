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
import moment from 'moment';
import { get } from 'lodash';
import PropTypes from 'prop-types';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';

import * as nodeApi from 'api/nodeApi';
import * as brokerApi from 'api/brokerApi';
import * as workerApi from 'api/workerApi';
import * as MESSAGES from 'constants/messages';
import { Modal } from 'components/common/Modal';
import * as commonUtils from 'utils/commonUtils';
import Checkbox from '@material-ui/core/Checkbox';
import { SortTable } from 'components/common/Mui/Table';
import { Main, NewButton, StyledTable } from '../styles';

const Nodes = props => {
  const { workspaceName } = props;
  const [useNodes, setUesNodes] = useState([]);
  const [broker, setBroker] = useState(null);
  const [unusedNodes, setUnusedNodes] = useState([]);
  const [selectNodes, setSelectNodes] = useState([]);
  const [order, setOrder] = useState('asc');
  const [orderBy, setOrderBy] = useState('name');
  const [loading, setLoading] = useState(true);
  const [confirmWorking, setConfirmWorking] = useState(false);
  const [nodeSelectOpen, setNodeSelectOpen] = useState(false);

  const fetchWorker = useCallback(async () => {
    const wkRes = await workerApi.fetchWorker(workspaceName);
    const nodeRes = await nodeApi.fetchNodes();
    const wkNodes = get(wkRes, 'data.result.nodeNames', []);
    const nodes = get(nodeRes, 'data.result', []);
    const nodeNames = nodes.map(node => node.name);
    const wkNodeNames = nodeNames.filter(x => wkNodes.includes(x));
    setBroker(wkRes.data.result.brokerClusterName);
    setUesNodes(nodes.filter(node => wkNodeNames.includes(node.name)));
    setUnusedNodes(nodes.filter(node => !wkNodeNames.includes(node.name)));
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

  const createData = (name, port, lastModified) => {
    return { name, port, lastModified };
  };

  const rows = useNodes.map(wk => {
    return createData(
      wk.name,
      wk.port,
      moment.unix(wk.lastModified / 1000).format('YYYY-MM-DD HH:mm:ss'),
    );
  });

  const handleRequestSort = (event, property) => {
    const isDesc = orderBy === property && order === 'desc';
    setOrder(isDesc ? 'asc' : 'desc');
    setOrderBy(property);
  };

  const handleNodeSelectClose = e => {
    setNodeSelectOpen(false);
  };

  const handleNodeSelect = e => {
    const { id, checked } = e.target;
    if (checked) {
      if (!selectNodes.some(node => node === id)) {
        selectNodes.push(id);
        setSelectNodes(selectNodes);
      }
    } else {
      if (selectNodes.some(node => node === id)) {
        const index = selectNodes.indexOf(id);
        selectNodes.splice(index, 1);
        setSelectNodes(selectNodes);
      }
    }
  };

  const waitForServiceCreation = async params => {
    const { retryCount = 0, type } = params;
    let res;
    if (type === 'worker') {
      res = await workerApi.fetchWorker(workspaceName);
    } else if (type === 'broker') {
      res = await brokerApi.fetchBroker(broker);
    }
    const nodeNames = get(res, 'data.result.nodeNames', []);
    if (retryCount > 5) return;
    if (nodeNames.some(n => selectNodes.includes(n))) {
      return;
    }
    await commonUtils.sleep(2000);
    await waitForServiceCreation(retryCount + 1);
  };

  const addNodeToService = async () => {
    for (let selectNode of selectNodes) {
      await brokerApi.addNodeToBroker({
        name: broker,
        nodeName: selectNode,
      });
      await waitForServiceCreation({ type: 'broker' });
      await workerApi.addNodeToWorker({
        name: workspaceName,
        nodeName: selectNode,
      });
      await waitForServiceCreation({ type: 'worker' });
    }
  };

  const handleAddNode = async () => {
    setConfirmWorking(true);
    if (selectNodes.length > 0) {
      await addNodeToService();
      await fetchWorker();
      toastr.success(MESSAGES.SERVICE_CREATION_SUCCESS);
      setSelectNodes([]);
    }
    setConfirmWorking(false);
    setNodeSelectOpen(false);
  };

  const headers = ['Select', 'Node name', 'Port'];

  return (
    <>
      <NewButton text="new node" onClick={() => setNodeSelectOpen(true)} />
      <Main>
        <SortTable
          isLoading={loading}
          headRows={headRows}
          rows={rows}
          onRequestSort={handleRequestSort}
          order={order}
          orderBy={orderBy}
        />
      </Main>
      <Modal
        title="Add node"
        isActive={nodeSelectOpen}
        width="400px"
        handleCancel={handleNodeSelectClose}
        handleConfirm={handleAddNode}
        confirmBtnText="Add"
        isConfirmWorking={confirmWorking}
      >
        <StyledTable headers={headers} isLoading={false}>
          {() => {
            return unusedNodes.map(node => {
              return (
                <TableRow key={node.name}>
                  <TableCell>
                    <Checkbox
                      id={node.name}
                      color="primary"
                      onChange={handleNodeSelect}
                    />
                  </TableCell>
                  <TableCell>{node.name}</TableCell>
                  <TableCell align="right">{node.port}</TableCell>
                </TableRow>
              );
            });
          }}
        </StyledTable>
      </Modal>
    </>
  );
};

Nodes.propTypes = {
  workspaceName: PropTypes.string.isRequired,
};

export default Nodes;
