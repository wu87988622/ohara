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
import { get, isNull, truncate } from 'lodash';

import * as jarApi from 'api/jarApi';
import { ListLoader } from 'components/common/Loader';
import { Modal } from 'components/common/Modal';
import { createConnector } from '../pipelineUtils/pipelineToolbarUtils';
import { TableWrapper, Table } from './styles';
import { Input, FormGroup } from 'components/common/Form';

const Inner = styled.div`
  padding: 30px 20px;
`;

const LoaderWrapper = styled.div`
  margin: 20px 40px;
`;

class PipelineNewStream extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      params: PropTypes.object.isRequired,
    }).isRequired,
    activeConnector: PropTypes.oneOfType([PropTypes.object, PropTypes.string]),
    updateGraph: PropTypes.func.isRequired,
    updateAddBtnStatus: PropTypes.func.isRequired,
    workerClusterName: PropTypes.string.isRequired,
    handleClose: PropTypes.func.isRequired,
  };

  state = {
    newStreamAppName: '',
    isModalOpen: false,
    pipelineId: null,
    isLoading: true,
    jars: [],
    activeJar: null,
    file: null,
    isDeleteRowModalActive: false,
    isTitleEditing: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const { match } = this.props;
    const pipelineId = get(match, 'params.pipelineId', null);
    this.setState({ pipelineId }, () => {
      this.fetchJars();
    });
  };

  handleTrSelect = params => {
    this.setState({
      activeJar: {
        group: params.group,
        name: params.name,
      },
    });
  };

  fetchJars = async () => {
    const { workerClusterName, updateAddBtnStatus } = this.props;
    const res = await jarApi.fetchJars(workerClusterName);
    this.setState({ isLoading: false });

    const jars = get(res, 'data.result', null);
    const activeJar = {
      group: get(jars, '[0].group', null),
      name: get(jars, '[0].name', null),
    };
    updateAddBtnStatus(activeJar);

    if (!isNull(jars)) {
      this.setState({ jars, activeJar });
    }
  };

  update = async () => {
    this.setState({ isModalOpen: true });
  };

  handleChange = ({ target: { value } }) => {
    const test = truncate(value.replace(/[^0-9a-z]/g, ''), {
      length: 30,
      omission: '',
    });
    this.setState({ newStreamAppName: test });
  };

  handleClose = () => {
    this.setState({ isModalOpen: false });
  };

  handleConfirm = () => {
    const connector = {
      jar: this.state.activeJar,
      className: 'streamApp',
      typeName: 'streamApp',
    };
    const { updateGraph } = this.props;
    const { newStreamAppName } = this.state;
    createConnector({
      updateGraph,
      connector,
      newStreamAppName,
    });
    this.props.handleClose();
  };

  render() {
    const {
      isModalOpen,
      isLoading,
      jars,
      activeJar,
      newStreamAppName,
    } = this.state;

    return (
      <div>
        {isLoading ? (
          <LoaderWrapper>
            <ListLoader />
          </LoaderWrapper>
        ) : (
          <React.Fragment>
            <TableWrapper>
              <Table headers={['FILENAME']}>
                {jars.map(({ group: id, name: title }) => {
                  const isActive = title === activeJar.name ? 'is-active' : '';
                  const params = { group: id, name: title };
                  return (
                    <tr
                      className={isActive}
                      key={id}
                      onClick={() => this.handleTrSelect(params)}
                      data-testid="stream-app-item"
                    >
                      <td>{title}</td>
                    </tr>
                  );
                })}
              </Table>
            </TableWrapper>
          </React.Fragment>
        )}
        <Modal
          isActive={isModalOpen}
          title="New StreamApp Name"
          width="370px"
          data-testid="addStreamApp"
          confirmBtnText="Add"
          handleConfirm={this.handleConfirm}
          handleCancel={this.handleClose}
        >
          <Inner>
            <FormGroup data-testid="name">
              <Input
                name="name"
                width="100%"
                placeholder="StreamApp name"
                data-testid="name-input"
                value={newStreamAppName}
                handleChange={this.handleChange}
              />
            </FormGroup>
          </Inner>
        </Modal>
      </div>
    );
  }
}

export default PipelineNewStream;
