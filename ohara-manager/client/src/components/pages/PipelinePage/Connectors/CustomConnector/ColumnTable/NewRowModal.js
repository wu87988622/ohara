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
import { Form, Field } from 'react-final-form';

import { Modal } from 'common/Modal';
import { FormGroup, Label } from 'common/Form';
import { FormInner } from './styles';
import { InputField, SelectField } from 'common/FormFields';

const NewRowModal = props => {
  const {
    isActive,

    dataTypes,

    handleConfirmClick,
    handleModalClose,
  } = props;
  return (
    <Form
      onSubmit={handleConfirmClick}
      initialValues={{ types: 'STRING' }}
      render={({ handleSubmit }) => {
        return (
          <Modal
            isActive={isActive}
            title="New row"
            width="290px"
            confirmBtnText="Create"
            handleConfirm={handleSubmit}
            handleCancel={handleModalClose}
          >
            <form>
              <FormInner>
                <FormGroup>
                  <Label>Column name</Label>
                  <Field
                    name="columnName"
                    width="100%"
                    placeholder="Column name"
                    component={InputField}
                  />
                </FormGroup>
                <FormGroup>
                  <Label>New column name</Label>
                  <Field
                    name="newColumnName"
                    width="100%"
                    placeholder="New column name"
                    component={InputField}
                  />
                </FormGroup>
                <FormGroup>
                  <Label>Type</Label>
                  <Field
                    name="types"
                    width="100%"
                    list={dataTypes}
                    component={SelectField}
                  />
                </FormGroup>
              </FormInner>
            </form>
          </Modal>
        );
      }}
    />
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
