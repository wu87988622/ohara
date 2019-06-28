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
import { endsWith, get, isNull } from 'lodash';

import * as streamApi from 'api/streamApi';
import { ListLoader } from 'components/common/Loader';
import { createConnector } from '../pipelineUtils/pipelineToolbarUtils';
import { TableWrapper, Table } from './styles';

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
  };

  state = {
    pipelineId: null,
    isLoading: true,
    jars: [],
    activeId: null,
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

  handleTrSelect = name => {
    this.setState({ activeId: name });
  };

  validateJarExtension = jarName => endsWith(jarName, '.jar');

  fetchJars = async () => {
    const { workerClusterName, updateAddBtnStatus } = this.props;
    const res = await streamApi.fetchJars(workerClusterName);
    this.setState({ isLoading: false });

    const jars = get(res, 'data.result', null);
    const activeId = get(jars, '[0].name', null);
    updateAddBtnStatus(activeId);

    if (!isNull(jars)) {
      this.setState({ jars, activeId });
    }
  };

  update = async () => {
    const connector = {
      jarName: this.state.activeId,
      className: 'streamApp',
      typeName: 'streamApp',
    };

    const { updateGraph } = this.props;
    createConnector({ updateGraph, connector });
  };

  render() {
    const { isLoading, jars, activeId } = this.state;

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
                {jars.map(({ name }) => {
                  const isActive = name === activeId ? 'is-active' : '';
                  return (
                    <tr
                      className={isActive}
                      key={name}
                      onClick={() => this.handleTrSelect(name)}
                      data-testid="stream-app-item"
                    >
                      <td>{name}</td>
                    </tr>
                  );
                })}
              </Table>
            </TableWrapper>
          </React.Fragment>
        )}
      </div>
    );
  }
}

export default PipelineNewStream;
