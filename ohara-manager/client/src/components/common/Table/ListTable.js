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
        <tr>{headers.map(header => <Th key={header}>{header}</Th>)}</tr>
      </thead>
      <tbody>
        {list.map(({ uuid, name }) => {
          return (
            <tr key={uuid}>
              <td>{name}</td>
              <td>
                <Link to={`${urlDir}/${uuid}`}>Details</Link>
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
