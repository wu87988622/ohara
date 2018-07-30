import React from 'react';
import PropTypes from 'prop-types';

const DataTable = ({ headers, data = [], handleDelete, hasAction = false }) => {
  return (
    <table className="table table-striped">
      <thead>
        <tr>{headers.map(header => <th key={header}>{header}</th>)}</tr>
      </thead>
      <tbody>
        {data.map(({ name, type }, idx) => {
          const seq = ++idx;
          return (
            <tr key={idx}>
              <td>{seq}</td>
              <td>{name}</td>
              <td>{type}</td>
              {hasAction && (
                <td>
                  <button
                    type="delete"
                    className="btn btn-danger"
                    onClick={() => handleDelete(name)}
                  >
                    Delete
                  </button>
                </td>
              )}
            </tr>
          );
        })}
      </tbody>
    </table>
  );
};

DataTable.propTypes = {
  prop: PropTypes.array,
};

export default DataTable;
