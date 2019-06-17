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
import * as PIPELINES from 'constants/pipelines';
import { ListLoader } from 'common/Loader';
import { Modal } from 'common/Modal';
import {
  createConnector,
  trimString,
} from '../pipelineUtils/pipelineToolbarUtils';
import { TableWrapper, Table } from './styles';
import { Input, FormGroup } from 'common/Form';

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
    isLoading: PropTypes.bool.isRequired,
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
    createConnector({
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
    const { connectors, activeConnector, onSelect, isLoading } = this.props;
    const { isModalOpen, newConnectorName } = this.state;

    return (
      <TableWrapper>
        {isLoading || !activeConnector ? (
          <ListLoader />
        ) : (
          <Table headers={PIPELINES.TABLE_HEADERS}>
            {connectors.map(({ className: name, version, revision }) => {
              const isActive =
                name === activeConnector.className ? 'is-active' : '';
              return (
                <tr
                  className={isActive}
                  key={name}
                  onClick={() => onSelect(name)}
                >
                  <td>{name}</td>
                  <td>{version}</td>
                  <td>{trimString(revision)}</td>
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
