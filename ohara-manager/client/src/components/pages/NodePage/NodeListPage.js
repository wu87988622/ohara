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
import DocumentTitle from 'react-document-title';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';
import { get } from 'lodash';

import * as s from './styles';
import NodeNewModal from './NodeNewModal';
import NodeEditModal from './NodeEditModal';
import EditIcon from '@material-ui/icons/Edit';
import IconButton from '@material-ui/core/IconButton';
import { NODES } from 'constants/documentTitles';
import { H2 } from 'components/common/Headings';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';

const NodeListPage = () => {
  const headers = ['HOST NAME', 'SERVICES', 'SSH', 'EDIT'];
  const [activeNode, setActiveNode] = useState(null);
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const { data: nodeRes, isLoading, refetch } = useApi.useFetchApi(
    URL.NODE_URL,
  );

  const handleEditClick = node => {
    setActiveNode(node);
    setIsEditModalOpen(true);
  };

  const handleNewModalOpen = () => {
    setIsNewModalOpen(true);
  };

  const handleModalClose = () => {
    setIsNewModalOpen(false);
    setIsEditModalOpen(false);
  };

  const getServiceNames = node => {
    if (node && node.services) {
      const result = node.services
        .reduce((acc, service) => {
          const { name, clusterNames } = service;
          const prefixedClusterNames = clusterNames.map(
            clusterName => `${clusterName}-${name}`,
          );

          return acc.concat(prefixedClusterNames);
        }, [])
        .join(', ');

      return result;
    }

    return [];
  };

  const getSSHLabel = (user, port) => {
    if (user && port) {
      return `user: ${user}, port: ${port}`;
    } else if (user) {
      return `user: ${user}`;
    } else if (port) {
      return `port: ${port}`;
    }
    return '';
  };

  return (
    <DocumentTitle title={NODES}>
      <>
        <s.Wrapper>
          <s.TopWrapper>
            <H2>Nodes</H2>
            <s.NewNodeBtn
              variant="contained"
              color="primary"
              text="New node"
              data-testid="new-node"
              onClick={handleNewModalOpen}
            />
          </s.TopWrapper>
          <s.NodeTable isLoading={isLoading} headers={headers}>
            {get(nodeRes, 'data.result', []).map(node => (
              <TableRow key={node.name}>
                <TableCell data-testid="node-name" component="th" scope="row">
                  {node.name || ''}
                </TableCell>
                <TableCell align="left">{getServiceNames(node)}</TableCell>
                <TableCell align="left">
                  {getSSHLabel(node.user, node.port)}
                </TableCell>
                <TableCell className="has-icon" align="right">
                  <IconButton
                    color="primary"
                    aria-label="Edit"
                    data-testid="edit-node-icon"
                    onClick={() => handleEditClick(node)}
                  >
                    <EditIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </s.NodeTable>
        </s.Wrapper>
        <NodeNewModal
          isOpen={isNewModalOpen}
          handleClose={handleModalClose}
          handleConfirm={() => {
            refetch();
          }}
        />
        <NodeEditModal
          node={activeNode}
          isOpen={isEditModalOpen}
          handleClose={handleModalClose}
          handleConfirm={() => {
            refetch();
          }}
        />
      </>
    </DocumentTitle>
  );
};

export default NodeListPage;
