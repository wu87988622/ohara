import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { darkerBlue, lighterGray, lightBlue } from '../../../theme/variables';

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

const DataTable = ({ headers, data }) => {
  if (!data) return null;
  return (
    <Table>
      <thead>
        <tr>{headers.map(header => <Th key={header}>{header}</Th>)}</tr>
      </thead>
      <tbody>
        {data.map(({ uuid, name, type }) => {
          return (
            <tr key={uuid}>
              <td>{name}</td>
              <td>{type}</td>
            </tr>
          );
        })}
      </tbody>
    </Table>
  );
};

DataTable.propTypes = {
  headers: PropTypes.arrayOf(PropTypes.string).isRequired,
  list: PropTypes.arrayOf(PropTypes.shape),
};

export default DataTable;
