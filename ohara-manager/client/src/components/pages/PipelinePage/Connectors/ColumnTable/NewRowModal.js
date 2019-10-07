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
import DialogContent from '@material-ui/core/DialogContent';
import FormControl from '@material-ui/core/FormControl';
import { Form, Field } from 'react-final-form';

import { Dialog } from 'components/common/Mui/Dialog';
import { InputField, Select } from 'components/common/Mui/Form';

const NewRowModal = props => {
  const { isActive, dataTypes, handleConfirmClick, handleModalClose } = props;
  return (
    <Form
      onSubmit={handleConfirmClick}
      initialValues={{}}
      render={({ handleSubmit, form, pristine, submitting, values }) => {
        const incomplete = Object.keys(values).length !== 3;

        return (
          <Dialog
            title="New row"
            open={isActive}
            maxWidth="xs"
            confirmDisabled={pristine || submitting || incomplete}
            handleConfirm={values => {
              handleSubmit(values);
              form.reset();
            }}
            handleClose={() => {
              form.reset();
              handleModalClose();
            }}
          >
            <DialogContent>
              <FormControl fullWidth margin="normal">
                <Field
                  autoFocus
                  name="columnName"
                  label="Column name"
                  placeholder="name"
                  component={InputField}
                  required
                />
              </FormControl>
              <FormControl fullWidth margin="normal">
                <Field
                  name="newColumnName"
                  label="New column name"
                  placeholder="newname"
                  component={InputField}
                  required
                />
              </FormControl>
              <FormControl fullWidth margin="normal">
                <Field
                  name="dataType"
                  label="Data type"
                  list={dataTypes}
                  component={Select}
                  required
                />
              </FormControl>
            </DialogContent>
          </Dialog>
        );
      }}
    />
  );
};

NewRowModal.propTypes = {
  isActive: PropTypes.bool.isRequired,
  dataTypes: PropTypes.array.isRequired,
  handleConfirmClick: PropTypes.func.isRequired,
  handleModalClose: PropTypes.func.isRequired,
};

export default NewRowModal;
