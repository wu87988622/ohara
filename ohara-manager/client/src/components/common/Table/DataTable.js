import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { darkerBlue, lighterGray, lightBlue } from '../../../theme/variables';

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  text-align: ${({ align }) => (align ? align : 'left')};

  th,
  td {
    font-size: 13px;
    padding: 20px 10px;
    border-bottom: 1px solid ${lighterGray};
  }

  td {
    color: ${lightBlue};

    &.has-icon {
      font-size: 20px;
    }
  }
`;

Table.displayName = 'Table';

const Th = styled.th`
  text-transform: uppercase;
  color: ${darkerBlue};
`;

Th.displayName = 'Th';

const DataTable = ({ headers, children, ...rest }) => {
  if (!children) return null;
  return (
    <Table {...rest}>
      <thead>
        <tr>{headers.map(header => <Th key={header}>{header}</Th>)}</tr>
      </thead>
      <tbody>{children}</tbody>
    </Table>
  );
};

DataTable.propTypes = {
  headers: PropTypes.arrayOf(PropTypes.string).isRequired,
  children: PropTypes.any,
};

export default DataTable;
