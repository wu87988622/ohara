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
import DocumentTitle from 'react-document-title';
import { reduce, map, sortBy, get, isNull, join } from 'lodash';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';

import * as nodeApi from 'api/nodeApi';
import { NODES } from 'constants/documentTitles';
import { H2 } from 'components/common/Headings';
import NodeNewModal from './NodeNewModal';
import NodeEditModal from './NodeEditModal';
import EditIcon from '@material-ui/icons/Edit';
import IconButton from '@material-ui/core/IconButton';

import * as s from './styles';

const NODE_EDIT_MODAL = 'nodeEditModal';

class NodeListPage extends React.Component {
  headers = ['HOST NAME', 'SERVICES', 'SSH', 'EDIT'];

  state = {
    isLoading: true,
    nodes: [],
    activeModal: null,
    activeNode: null,
    isNewModalOpen: false,
    isEditModalOpen: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const res = await nodeApi.fetchNodes();
    this.setState(() => ({ isLoading: false }));
    const nodes = get(res, 'data.result', null);
    if (!isNull(nodes)) {
      this.setState({ nodes: sortBy(nodes, 'name') });
    }
  };

  handleEditClick = node => {
    this.setState({
      activeModal: NODE_EDIT_MODAL,
      activeNode: node,
      isEditModalOpen: true,
    });
  };

  handleNewModalOpen = () => {
    this.setState({ isNewModalOpen: true });
  };

  handleModalColse = () => {
    this.setState({ isNewModalOpen: false, isEditModalOpen: false });
  };

  getAllClusterNames = node => {
    if (node && node.services) {
      return reduce(
        node.services,
        (results, service) => {
          map(service.clusterNames, clusterName => {
            results.push(clusterName);
          });
          return results;
        },
        [],
      );
    }
    return [];
  };

  getSSHLabel = (user, port) => {
    if (user && port) {
      return `user: ${user}, port: ${port}`;
    } else if (user) {
      return `user: ${user}`;
    } else if (port) {
      return `port: ${port}`;
    }
    return '';
  };

  render() {
    const { nodes, isLoading, activeNode } = this.state;

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
                onClick={() => {
                  this.handleNewModalOpen();
                }}
              />
            </s.TopWrapper>
            <s.NodeTable isLoading={isLoading} headers={this.headers}>
              {() => {
                return nodes.map(node => (
                  <TableRow key={node.name}>
                    <TableCell component="th" scope="row">
                      {node.name || ''}
                    </TableCell>
                    <TableCell align="left">
                      {join(this.getAllClusterNames(node), ', ')}
                    </TableCell>
                    <TableCell align="left">
                      {this.getSSHLabel(node.user, node.port)}
                    </TableCell>
                    <TableCell className="has-icon" align="right">
                      <IconButton
                        color="primary"
                        aria-label="Edit"
                        data-testid="edit-node-icon"
                        onClick={() => this.handleEditClick(node)}
                      >
                        <EditIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ));
              }}
            </s.NodeTable>
          </s.Wrapper>
          <NodeNewModal
            isOpen={this.state.isNewModalOpen}
            handleClose={this.handleModalColse}
            handleConfirm={this.fetchData}
          />
          <NodeEditModal
            node={activeNode}
            isOpen={this.state.isEditModalOpen}
            handleClose={this.handleModalColse}
            handleConfirm={this.fetchData}
          />
        </>
      </DocumentTitle>
    );
  }
}

export default NodeListPage;
