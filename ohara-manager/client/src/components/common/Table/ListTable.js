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
import { Link } from 'react-router-dom';

import { darkerBlue, lighterGray, lightBlue } from 'theme/variables';

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;

  th,
  td {
    font-size: 13px;
    padding: 20px 10px;
    border-bottom: 1px solid ${lighterGray};
  }

  td {
    color: ${lightBlue};
  }
`;

Table.displayName = 'Table';

const Th = styled.th`
  text-transform: uppercase;
  text-align: left;
  color: ${darkerBlue};
`;

Th.displayName = 'Th';

const ListTable = ({ headers, list, urlDir }) => {
  if (!list) return null;
  return (
    <Table>
      <thead>
        <tr>
          {headers.map(header => (
            <Th key={header}>{header}</Th>
          ))}
        </tr>
      </thead>
      <tbody>
        {list.map(({ id, name }) => {
          return (
            <tr key={id}>
              <td>{name}</td>
              <td>
                <Link to={`${urlDir}/${id}`}>Details</Link>
              </td>
            </tr>
          );
        })}
      </tbody>
    </Table>
  );
};

ListTable.propTypes = {
  headers: PropTypes.arrayOf(PropTypes.string).isRequired,
  list: PropTypes.arrayOf(PropTypes.shape).isRequired,
  urlDir: PropTypes.string,
};

export default ListTable;
