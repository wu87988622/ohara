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
import PropTypes from 'prop-types';
import styled from 'styled-components';

import * as utils from './pipelineToolbarUtils';
import { Modal } from 'components/common/Modal';
import { ListLoader } from 'components/common/Loader';
import { TableWrapper, Table } from './styles';
import { Input, FormGroup } from 'components/common/Form';

const Inner = styled.div`
  padding: 30px 20px;
`;

class PipelineNewConnector extends React.Component {
  static propTypes = {
    connectors: PropTypes.array.isRequired,
    onSelect: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
    activeConnector: PropTypes.object,
    updateAddBtnStatus: PropTypes.func.isRequired,
    workerClusterName: PropTypes.string.isRequired,
    handleClose: PropTypes.func.isRequired,
  };

  state = {
    isModalOpen: false,
    newConnectorName: '',
  };

  componentDidMount() {
    this.props.updateAddBtnStatus(this.props.activeConnector);
  }

  update = () => {
    this.setState({ isModalOpen: true });
  };

  handleChange = ({ target: { value } }) => {
    this.setState({ newConnectorName: value });
  };

  handleConfirm = () => {
    const {
      updateGraph,
      activeConnector: connector,
      workerClusterName,
    } = this.props;
    const { newConnectorName } = this.state;

    utils.createConnector({
      updateGraph,
      connector,
      workerClusterName,
      newConnectorName,
    });

    this.props.handleClose();
  };

  handleClose = () => {
    this.setState({ isModalOpen: false });
  };

  render() {
    const { connectors, activeConnector, onSelect } = this.props;
    const { isModalOpen, newConnectorName } = this.state;

    return (
      <TableWrapper>
        {!activeConnector ? (
          <ListLoader />
        ) : (
          <Table headers={['connector name', 'version', 'revision']}>
            {connectors.map(({ className: name, version, revision }) => {
              const isActive =
                name === activeConnector.className ? 'is-active' : '';
              return (
                <tr
                  className={isActive}
                  key={name}
                  onClick={() => onSelect(name)}
                  data-testid="connector-list"
                >
                  <td>{name}</td>
                  <td>{version}</td>
                  <td>{utils.trimString(revision)}</td>
                </tr>
              );
            })}
          </Table>
        )}
        <Modal
          isActive={isModalOpen}
          title="New Connector Name"
          width="370px"
          confirmBtnText="Add"
          handleConfirm={this.handleConfirm}
          handleCancel={this.handleClose}
        >
          <Inner>
            <FormGroup data-testid="name">
              <Input
                name="name"
                width="100%"
                placeholder="Connector name"
                data-testid="name-input"
                value={newConnectorName}
                handleChange={this.handleChange}
              />
            </FormGroup>
          </Inner>
        </Modal>
      </TableWrapper>
    );
  }
}

export default PipelineNewConnector;
