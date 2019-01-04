import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { lighterGray } from 'theme/variables';

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;

  th,
  td {
    text-align: center;
    font-size: 13px;
    padding: 20px 10px;
    border-bottom: 1px solid ${lighterGray};
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
                  <a href="" onClick={e => handleUp(e, order)}>
                    <i className="fas fa-arrow-up" />
                  </a>
                </td>
                <td>
                  <a href="" onClick={e => handleDown(e, order)}>
                    <i className="fas fa-arrow-down" />
                  </a>
                </td>
                <td>
                  <a href="" onClick={e => handleModalOpen(e, order)}>
                    <i className="far fa-trash-alt" />
                  </a>
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
