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
import { DataTable } from 'common/Table';
import { lighterBlue, lightBlue, trBgColor } from 'theme/variables';
import { createConnector } from './pipelineUtils/pipelineToolbarUtils';

const TableWrapper = styled.div`
  margin: 30px 30px 40px;
`;

const Table = styled(DataTable)`
  thead th {
    color: ${lightBlue};
    font-weight: normal;
  }

  td {
    color: ${lighterBlue};
  }

  tbody tr {
    cursor: pointer;
  }

  .is-active {
    background-color: ${trBgColor};
  }
`;

class PipelineNewConnector extends React.Component {
  static propTypes = {
    connectors: PropTypes.array.isRequired,
    onSelect: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
    activeConnector: PropTypes.object,
  };

  update = () => {
    const { updateGraph, activeConnector: connector } = this.props;
    createConnector({ updateGraph, connector });
  };

  trimString = string => {
    // https://stackoverflow.com/a/18134919/1727948
    // Only displays the first 8 digits of the git sha instead of the full number so
    // it won't break our layout
    return string.substring(0, 7);
  };

  render() {
    const { connectors, activeConnector, onSelect } = this.props;

    if (!activeConnector) return null;

    return (
      <TableWrapper>
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
                <td>{this.trimString(revision)}</td>
              </tr>
            );
          })}
        </Table>
      </TableWrapper>
    );
  }
}

export default PipelineNewConnector;
