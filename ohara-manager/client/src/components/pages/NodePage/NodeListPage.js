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
import { reduce, map, sortBy, get, isNull } from 'lodash';

import * as nodeApi from 'api/nodeApi';
import { NODES } from 'constants/documentTitles';
import { Box } from 'components/common/Layout';
import { H2 } from 'components/common/Headings';
import MuiNewModal from './MuiNewModal';
import MuiEditModal from './MuiEditModal';

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
        <React.Fragment>
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
            <Box>
              <s.NodeTable
                getAllClusterNames={this.getAllClusterNames}
                getSSHLabel={this.getSSHLabel}
                handleEditClick={this.handleEditClick}
                nodes={nodes}
                isLoading={isLoading}
                headers={this.headers}
              />
            </Box>
          </s.Wrapper>
          <MuiNewModal
            isOpen={this.state.isNewModalOpen}
            handleClose={this.handleModalColse}
            handleConfirm={this.fetchData}
          />
          <MuiEditModal
            node={activeNode}
            isOpen={this.state.isEditModalOpen}
            handleClose={this.handleModalColse}
            handleConfirm={this.fetchData}
          />
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default NodeListPage;
