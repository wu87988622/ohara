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
import DialogContentText from '@material-ui/core/DialogContentText';
import { capitalize } from 'lodash';
import { Form, Field } from 'react-final-form';

import { InputField } from 'components/common/Form';
import { Dialog } from 'components/common/Dialog';
import { required, validServiceName, composeValidators } from 'utils/validate';

const ToolboxAddGraphDialog = props => {
  const { isOpen, onClose, onConfirm, kind } = props;

  const onSubmit = async (values, form) => {
    await onConfirm(values.newGraph);
    setTimeout(form.reset);
  };

  return (
    <Form
      onSubmit={onSubmit}
      initialValues={{}}
      render={({ handleSubmit, form, submitting, pristine, invalid }) => {
        return (
          <Dialog
            open={isOpen}
            onClose={() => {
              onClose();
              form.reset();
            }}
            onConfirm={handleSubmit}
            title={`Add a new ${kind}`}
            confirmText="ADD"
            confirmDisabled={submitting || pristine || invalid}
            loading={submitting}
            maxWidth="xs"
          >
            <DialogContentText>
              Please note that once the name is added, it will become
              "Read-only".
            </DialogContentText>

            <form onSubmit={handleSubmit}>
              <Field
                id={`add-${kind}-name`}
                type="text"
                name="newGraph"
                label={`${capitalize(kind)} Name`}
                component={InputField}
                validate={composeValidators(required, validServiceName)}
                disabled={submitting}
                autoFocus
                required
              />
            </form>
          </Dialog>
        );
      }}
    />
  );
};

ToolboxAddGraphDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
  kind: PropTypes.string.isRequired,
};

export default ToolboxAddGraphDialog;
