import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Link } from 'react-router-dom';

const TableRow = styled.tr`
  background-color: #81bef7;
`;

const Table = ({ headers, list, urlDir }) => {
  return (
    <table className="table table-striped">
      <thead>
        <TableRow className="text-white">
          {headers.map(header => <th key={header}>{header}</th>)}
        </TableRow>
      </thead>
      <tbody>
        {list.map(item => {
          const key = Object.keys(item)[0];
          const value = Object.values(item)[0];

          return (
            <tr key={key}>
              <td>{value}</td>
              <td>
                <Link to={`${urlDir}/${value}`}>Details</Link>
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
};

Table.propTypes = {
  headers: PropTypes.arrayOf(PropTypes.string).isRequired,
  list: PropTypes.arrayOf(PropTypes.shape).isRequired,
  urlDir: PropTypes.string.isRequired,
};

export default Table;
