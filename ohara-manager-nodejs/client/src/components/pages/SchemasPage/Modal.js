import React from 'react';
import PropTypes from 'prop-types';
import ReactModal from 'react-modal';
import styled from 'styled-components';

const modalStyles = {
  content: {
    top: '15%',
    left: '50%',
    right: 'auto',
    bottom: 'auto',
    marginRight: '-50%',
    transform: 'translate(-50%, 0)',
  },
};

const FormWrapper = styled.form`
  margin-bottom: 20px;
`;

const H4 = styled.h4`
  margin-bottom: 15px;
`;

const Icon = styled.i`
  position: absolute;
  right: 20px;
  top: 15px;
  cursor: pointer;
  padding: 8px;
`;

const Label = styled.label`
  margin-right: 5px;
`;

const InlineWrapper = styled.span`
  margin-right: 10px;
`;

const Modal = ({
  isActive,
  schemaName,
  columnName,
  currDataType,
  dataTypes,
  schemasColumns,
  handleChangeSelect,
  handleCloseModal,
  handleChangeInput,
  handleAddColumn,
  handleDeleteColumn,
  handleSave,
}) => {
  const tableHeaders = ['#', 'Column Name', 'Data Type', 'Action'];

  return (
    <ReactModal
      isOpen={isActive}
      contentLabel="Modal"
      ariaHideApp={false}
      style={modalStyles}
      onRequestClose={handleCloseModal}
    >
      <Icon className="fas fa-times text-muted" onClick={handleCloseModal} />
      <H4>Create a schema</H4>

      <FormWrapper>
        <div>
          <Label>Schema name</Label>
          <input
            type="text"
            name="schemaName"
            onChange={handleChangeInput}
            value={schemaName}
          />
        </div>

        <div>
          <InlineWrapper>
            <Label>Column name</Label>
            <input
              type="text"
              name="columnName"
              onChange={handleChangeInput}
              value={columnName}
            />
          </InlineWrapper>

          <InlineWrapper>
            <Label>Data type</Label>
            <select value={currDataType} onChange={handleChangeSelect}>
              {dataTypes.map(type => {
                return (
                  <option key={type} value={type}>
                    {type}
                  </option>
                );
              })}
            </select>
          </InlineWrapper>
          <InlineWrapper>
            <Label>Disable</Label>
            <input type="checkbox" name="isDisable" />
          </InlineWrapper>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleAddColumn}
          >
            Add
          </button>
        </div>
      </FormWrapper>

      <table className="table table-striped">
        <thead>
          <tr>{tableHeaders.map(header => <th key={header}>{header}</th>)}</tr>
        </thead>
        <tbody>
          {schemasColumns.map(({ name, type }, idx) => {
            const seq = ++idx;
            return (
              <tr key={idx}>
                <td>{seq}</td>
                <td>{name}</td>
                <td>{type}</td>
                <td>
                  <button
                    type="delete"
                    className="btn btn-danger"
                    onClick={() => handleDeleteColumn(name)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>

      <div className="float-right">
        <button type="button" className="btn btn-primary" onClick={handleSave}>
          Save
        </button>
        <button
          type="button"
          className="btn btn-default"
          onClick={handleCloseModal}
        >
          Close
        </button>
      </div>
    </ReactModal>
  );
};

Modal.propTypes = {
  isActive: PropTypes.bool.isRequired,
  schemaName: PropTypes.string.isRequired,
  columnName: PropTypes.string.isRequired,
  currDataType: PropTypes.string.isRequired,
  dataTypes: PropTypes.arrayOf(PropTypes.string).isRequired,
  schemasColumns: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string,
      type: PropTypes.string,
    }),
  ).isRequired,
};

export default Modal;
