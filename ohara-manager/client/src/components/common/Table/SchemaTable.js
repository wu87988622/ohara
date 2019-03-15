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

import { LinkButton } from 'common/Form';

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;

  th,
  td {
    text-align: center;
    font-size: 13px;
    padding: 20px 10px;
    border-bottom: 1px solid ${props => props.theme.lighterGray};
  }
`;

class SchemaTable extends React.Component {
  static propTypes = {
    headers: PropTypes.array.isRequired,
    schema: PropTypes.arrayOf(
      PropTypes.shape({
        order: PropTypes.number,
        name: PropTypes.string,
        newName: PropTypes.string,
        dataType: PropTypes.string,
      }),
    ),
    dataTypes: PropTypes.array,
    handleModalOpen: PropTypes.func,
    handleTypeChange: PropTypes.func,
    handleUp: PropTypes.func,
    handleDown: PropTypes.func,
  };

  render() {
    const {
      headers,
      schema,
      handleModalOpen,
      handleTypeChange,
      handleUp,
      handleDown,
      dataTypes,
    } = this.props;

    return (
      <Table>
        <thead>
          <tr>
            {headers.map(header => (
              <th key={header}>{header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {schema.map(({ order, name, newName, dataType: currType }) => {
            return (
              <tr key={order}>
                <td>{order}</td>
                <td>{name}</td>
                <td>{newName}</td>
                <td>
                  <select
                    onChange={e => handleTypeChange(e, order)}
                    value={currType}
                  >
                    {dataTypes.map(dataType => {
                      return <option key={dataType}>{dataType}</option>;
                    })}
                  </select>
                </td>
                <td>
                  <LinkButton handleClick={e => handleUp(e, order)}>
                    <i className="fas fa-arrow-up" />
                  </LinkButton>
                </td>
                <td>
                  <LinkButton handleClick={e => handleDown(e, order)}>
                    <i className="fas fa-arrow-down" />
                  </LinkButton>
                </td>
                <td>
                  <LinkButton handleClick={e => handleModalOpen(e, order)}>
                    <i className="far fa-trash-alt" />
                  </LinkButton>
                </td>
              </tr>
            );
          })}
        </tbody>
      </Table>
    );
  }
}

export default SchemaTable;
