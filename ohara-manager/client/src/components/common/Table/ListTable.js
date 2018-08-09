import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Link } from 'react-router-dom';

import { darkerBlue, lighterGray } from '../../../theme/variables';

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;

  th,
  td {
    padding: 20px 0;
    border-bottom: 1px solid ${lighterGray};
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
  return (
    <Table>
      <thead>
        <tr>{headers.map(header => <Th key={header}>{header}</Th>)}</tr>
      </thead>
      <tbody>
        {list.map(item => {
          const key = Object.keys(item)[0];
          const value = Object.values(item)[0];

          return (
            <tr key={key}>
              <td>{value}</td>
              <td>
                <Link to={`${urlDir}/${key}`}>Details</Link>
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
