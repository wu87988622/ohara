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
import { DataTable } from 'common/Table';
import {
  createConnector,
  trimString,
} from '../pipelineUtils/pipelineToolbarUtils';

const TableWrapper = styled.div`
  margin: 30px 30px 40px;
`;

const Table = styled(DataTable)`
  thead th {
    color: ${props => props.theme.lightBlue};
    font-weight: normal;
  }

  td {
    color: ${props => props.theme.lighterBlue};
  }

  tbody tr {
    cursor: pointer;
  }

  .is-active {
    background-color: ${props => props.theme.trBgColor};
  }
`;

class PipelineNewConnector extends React.Component {
  static propTypes = {
    connectors: PropTypes.array.isRequired,
    onSelect: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
    activeConnector: PropTypes.object,
    updateAddBtnStatus: PropTypes.func.isRequired,
    isLoading: PropTypes.bool.isRequired,
  };

  componentDidMount() {
    this.props.updateAddBtnStatus(this.props.activeConnector);
  }

  update = () => {
    const { updateGraph, activeConnector: connector } = this.props;
    createConnector({ updateGraph, connector });
  };

  render() {
    const { connectors, activeConnector, onSelect, isLoading } = this.props;

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
      </TableWrapper>
    );
  }
}

export default PipelineNewConnector;
