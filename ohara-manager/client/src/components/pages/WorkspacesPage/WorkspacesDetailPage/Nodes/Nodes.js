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
import { Modal } from 'components/common/Mui/Dialog';
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
  const [confirmDisabled, setConfirmDisabled] = useState(false);
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
        setConfirmDisabled(false);
      }
    } else {
      if (selectNodes.some(node => node === id)) {
        const index = selectNodes.indexOf(id);
        selectNodes.splice(index, 1);
        setSelectNodes(selectNodes);
      }
      if (selectNodes.length === 0) {
        setConfirmDisabled(true);
      }
    }
  };

  const waitForServiceCreation = async retryCount => {
    const res = await workerApi.fetchWorker(workspaceName);
    const nodeNames = get(res, 'data.result.nodeNames', []);
    if (retryCount > 5) return;
    if (nodeNames.some(n => selectNodes.includes(n))) {
      toastr.success(MESSAGES.SERVICE_CREATION_SUCCESS);
      return;
    }
    await commonUtils.sleep(2000);
    await waitForServiceCreation(retryCount + 1);
  };

  const handelAddNode = () => {
    if (selectNodes.length > 0) {
      selectNodes.map(async selectNode => {
        const bkParams = {
          name: broker,
          nodeName: selectNode,
        };
        const wkParams = {
          name: workspaceName,
          nodeName: selectNode,
        };
        await brokerApi.addNodeToBroker(bkParams);
        await workerApi.addNodeToWorker(wkParams);
        await waitForServiceCreation(0);
        setNodeSelectOpen(false);
        fetchWorker();
      });
      setSelectNodes([]);
    }
  };

  const handelOpen = () => {
    if (selectNodes.length === 0) {
      setConfirmDisabled(true);
    } else {
      setConfirmDisabled(false);
    }
    setNodeSelectOpen(true);
  };

  const headers = ['Select', 'Node name', 'Port'];

  return (
    <>
      <NewButton text="new node" onClick={handelOpen} />
      <Main>
        <SortTable
          isLoading={loading}
          headRows={headRows}
          rows={rows}
          onRequestSort={handleRequestSort}
          order={order}
          orderBy={orderBy}
          confirmDisabled={confirmDisabled}
        />
      </Main>

      <Modal
        handelOpen={nodeSelectOpen}
        handelClose={handleNodeSelectClose}
        title="Add Node"
        handleConfirm={handelAddNode}
        confirmDisabled={confirmDisabled}
      >
        {() => {
          return (
            <StyledTable headers={headers} isLoading={false}>
              {() => {
                return (
                  <>
                    {unusedNodes.map(node => {
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
                    })}
                    {useNodes.map(node => {
                      return (
                        <TableRow key={node.name} selected>
                          <TableCell>
                            <Checkbox
                              id={node.name}
                              color="primary"
                              onChange={handleNodeSelect}
                              checked
                              disabled
                            />
                          </TableCell>
                          <TableCell>{node.name}</TableCell>
                          <TableCell align="right">{node.port}</TableCell>
                        </TableRow>
                      );
                    })}
                  </>
                );
              }}
            </StyledTable>
          );
        }}
      </Modal>
    </>
  );
};

Nodes.propTypes = {
  workspaceName: PropTypes.string.isRequired,
};

export default Nodes;
