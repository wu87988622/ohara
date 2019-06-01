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

import NewRowModal from './NewRowModal';
import Table from './Table';
import { ConfirmModal } from 'common/Modal';
import { NewRowBtn } from './styles';

class ColumnTable extends React.Component {
  static propTypes = {
    headers: PropTypes.array.isRequired,
    data: PropTypes.arrayOf(
      PropTypes.shape({
        order: PropTypes.number.isRequired,
        dataType: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        newName: PropTypes.string.isRequired,
      }),
    ),
    dataTypes: PropTypes.array.isRequired,
    handleTypeChange: PropTypes.func,
    handleUp: PropTypes.func,
    handleDown: PropTypes.func,
    handleColumnChange: PropTypes.func.isRequired,
    handleColumnRowDelete: PropTypes.func.isRequired,
    handleColumnRowUp: PropTypes.func.isRequired,
    handleColumnRowDown: PropTypes.func.isRequired,
    parentValues: PropTypes.object,
  };

  initialState = {
    isNewRowModalActive: false,
    isDeleteRowModalActive: false,
    columnName: '',
    newColumnName: '',
    currType: '',
    currRow: 0,
  };

  state = this.initialState;

  componentDidMount() {
    this.setState({ currType: this.props.dataTypes[0] });
  }

  handleChange = ({ target }) => {
    const { name, value } = target;
    this.setState({ [name]: value });
  };

  handleNewRowModalOpen = e => {
    e.preventDefault();
    this.setState({ isNewRowModalActive: true });
  };

  handleNewRowModalClose = () => {
    this.setState(this.initialState);
  };

  handleNewRow = values => {
    const { handleColumnChange, parentValues } = this.props;
    const { columnName, newColumnName, types: currType } = values;
    this.setState({ columnName, newColumnName, currType });
    handleColumnChange({ parentValues, columnName, newColumnName, currType });
    this.handleNewRowModalClose();
  };

  handleDeleteRow = () => {
    this.props.handleColumnRowDelete({
      currRow: this.state.currRow,
      parentValues: this.props.parentValues,
    });
    this.handleDeleteRowModalClose();
  };

  handleDeleteRowModalOpen = (e, order) => {
    e.preventDefault();
    this.setState({ isDeleteRowModalActive: true, currRow: order });
  };

  handleDeleteRowModalClose = () => {
    this.setState({ isDeleteRowModalActive: false, currRow: 0 });
  };

  render() {
    const {
      headers,
      data,
      handleUp,
      handleDown,
      dataTypes,
      handleColumnRowUp,
      handleColumnRowDown,
      parentValues,
    } = this.props;

    const {
      isNewRowModalActive,
      columnName,
      newColumnName,
      handleTypeChange,
      currType,
      isDeleteRowModalActive,
    } = this.state;

    return (
      <>
        <NewRowBtn
          text="New row"
          size="small"
          data-testid="new-row-btn"
          onClick={this.handleNewRowModalOpen}
        />

        <NewRowModal
          isActive={isNewRowModalActive}
          columnName={columnName}
          newColumnName={newColumnName}
          dataTypes={dataTypes}
          currDataType={currType}
          handleChange={this.handleChange}
          handleConfirmClick={this.handleNewRow}
          handleModalClose={this.handleNewRowModalClose}
        />

        <ConfirmModal
          isActive={isDeleteRowModalActive}
          title="Delete row?"
          confirmBtnText="Yes, Delete this row"
          cancelBtnText="No, Keep it"
          handleCancel={this.handleDeleteRowModalClose}
          handleConfirm={this.handleDeleteRow}
          message="Are you sure you want to delete this row? This action cannot be undone!"
          isDelete
        />

        <Table
          headers={headers}
          data={data}
          dataTypes={dataTypes}
          handleUp={handleUp}
          handleDown={handleDown}
          handleTypeChange={handleTypeChange}
          handleDeleteRowModalOpen={this.handleDeleteRowModalOpen}
          handleColumnRowUp={handleColumnRowUp}
          handleColumnRowDown={handleColumnRowDown}
          parentValues={parentValues}
        />
      </>
    );
  }
}

export default ColumnTable;
