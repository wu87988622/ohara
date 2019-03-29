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
import { Box } from 'common/Layout';
import { H2 } from 'common/Headings';
import NodeNewModal from './NodeNewModal';
import NodeEditModal from './NodeEditModal';
import MuiTable from './NodeListTable';
import MuiNewModal from './MuiNewModal';

import * as s from './Styles';

const NODE_NEW_MODAL = 'nodeNewModal';
const NODE_EDIT_MODAL = 'nodeEditModal';

class NodeListPage extends React.Component {
  state = {
    isLoading: true,
    nodes: [],
    activeModal: null,
    activeNode: null,
    newOpen: false,
    editOpen: false,
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

  handleNewClickOpen = () => {
    this.setState({ newOpen: true });
  };

  handleEditClickOpen = () => {
    this.setState({ editOpen: true });
  };

  handleClose = () => {
    this.setState({ newOpen: false, editOpen: false });
  };

  handleEditClick = node => {
    this.setState({
      activeModal: NODE_EDIT_MODAL,
      activeNode: node,
    });
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
    const { nodes, isLoading, activeModal, activeNode } = this.state;
    return (
      <DocumentTitle title={NODES}>
        <React.Fragment>
          <s.Wrapper>
            <s.TopWrapper>
              <H2>Nodes</H2>
              <s.MuiBtn
                color="primary"
                variant="contained"
                data-testid="new-node"
                onClick={this.handleNewClickOpen}
              >
                New Node
              </s.MuiBtn>
              <s.MuiBtn
                color="primary"
                variant="contained"
                data-testid="new-node"
                onClick={() => {
                  this.setState({ activeModal: NODE_NEW_MODAL });
                }}
              >
                New Node2
              </s.MuiBtn>
            </s.TopWrapper>
            <Box>
              <MuiTable
                getAllClusterNames={this.getAllClusterNames}
                getSSHLabel={this.getSSHLabel}
                handleEditClick={this.handleEditClick}
                nodes={nodes}
                isLoading={isLoading}
              />
            </Box>
          </s.Wrapper>
          <MuiNewModal
            newOpen={this.state.newOpen}
            handleClose={this.handleClose}
            handleConfirm={this.fetchData}
          />
          <NodeNewModal
            isActive={activeModal === NODE_NEW_MODAL}
            handleClose={() => {
              this.setState({ activeModal: null });
            }}
            handleConfirm={this.fetchData}
          />
          <NodeEditModal
            node={activeNode}
            isActive={activeModal === NODE_EDIT_MODAL}
            handleClose={() => {
              this.setState({ activeModal: null, activeNode: null });
            }}
            handleConfirm={this.fetchData}
          />
        </React.Fragment>
      </DocumentTitle>
    );
  }
}

export default NodeListPage;
