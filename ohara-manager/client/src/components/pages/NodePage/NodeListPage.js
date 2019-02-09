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
import { reduce, map, join, sortBy, get, isNull } from 'lodash';

import * as nodeApi from 'api/nodeApi';
import { NODES } from 'constants/documentTitles';
import { Box } from 'common/Layout';
import { H2 } from 'common/Headings';
import { primaryBtn } from 'theme/btnTheme';
import NodeNewModal from './NodeNewModal';
import NodeEditModal from './NodeEditModal';

import * as s from './Styles';

const NODE_NEW_MODAL = 'nodeNewModal';
const NODE_EDIT_MODAL = 'nodeEditModal';

class NodeListPage extends React.Component {
  headers = ['HOST NAME', 'SERVICES', 'SSH', 'EDIT'];

  state = {
    isLoading: true,
    nodes: [],
    activeModal: null,
    activeNode: null,
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
              <s.NewNodeBtn
                theme={primaryBtn}
                text="New node"
                data-testid="new-node"
                handleClick={() => {
                  this.setState({ activeModal: NODE_NEW_MODAL });
                }}
              />
            </s.TopWrapper>
            <Box>
              <s.Table headers={this.headers} isLoading={isLoading}>
                {nodes.map(node => (
                  <tr key={node.name}>
                    <td>{node.name || ''}</td>
                    <td>{join(this.getAllClusterNames(node), ', ')}</td>
                    <td>{this.getSSHLabel(node.user, node.port)}</td>
                    <td className="has-icon">
                      <s.Icon
                        className="far fa-edit"
                        onClick={() => this.handleEditClick(node)}
                      />
                    </td>
                  </tr>
                ))}
              </s.Table>
            </Box>
          </s.Wrapper>
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
