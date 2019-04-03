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

import { Modal } from 'common/Modal';
import { Input, Select, FormGroup, Label } from 'common/Form';
import { FormInner } from './styles';

const NewRowModal = props => {
  const {
    isActive,
    columnName,
    newColumnName,
    dataTypes,
    currDataType,
    handleChange,
    handleConfirmClick,
    handleModalClose,
  } = props;

  return (
    <Modal
      isActive={isActive}
      title="New row"
      width="290px"
      confirmBtnText="Create"
      handleConfirm={handleConfirmClick}
      handleCancel={handleModalClose}
    >
      <form>
        <FormInner>
          <FormGroup>
            <Label>Column name</Label>
            <Input
              name="columnName"
              width="100%"
              placeholder="Column name"
              value={columnName}
              handleChange={handleChange}
            />
          </FormGroup>
          <FormGroup>
            <Label>New column name</Label>
            <Input
              name="newColumnName"
              width="100%"
              placeholder="New column name"
              value={newColumnName}
              handleChange={handleChange}
            />
          </FormGroup>
          <FormGroup>
            <Label>Type</Label>
            <Select
              name="types"
              width="100%"
              list={dataTypes}
              selected={currDataType}
              handleChange={handleChange}
            />
          </FormGroup>
        </FormInner>
      </form>
    </Modal>
  );
};

NewRowModal.propTypes = {
  isActive: PropTypes.bool.isRequired,
  columnName: PropTypes.string.isRequired,
  newColumnName: PropTypes.string.isRequired,
  dataTypes: PropTypes.array.isRequired,
  currDataType: PropTypes.string.isRequired,
  handleChange: PropTypes.func.isRequired,
  handleConfirmClick: PropTypes.func.isRequired,
  handleModalClose: PropTypes.func.isRequired,
};

export default NewRowModal;
