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
import {
  required,
  validServiceName,
  composeValidators,
  maxLength,
  checkDuplicate,
} from 'utils/validate';

const ToolboxAddGraphDialog = props => {
  const { isOpen, onClose, onConfirm, kind, paperElementNames } = props;

  const onSubmit = async (values, form) => {
    await onConfirm(values.newGraph);
    setTimeout(form.reset);
  };

  return (
    <Form
      initialValues={{}}
      onSubmit={onSubmit}
      render={({ handleSubmit, form, submitting, pristine, invalid }) => {
        return (
          <Dialog
            confirmDisabled={submitting || pristine || invalid}
            confirmText="ADD"
            loading={submitting}
            maxWidth="xs"
            onClose={() => {
              onClose();
              form.reset();
            }}
            onConfirm={handleSubmit}
            open={isOpen}
            title={`Add a new ${kind}`}
          >
            <DialogContentText>
              Please note that once the name is added, it will become
              "Read-only".
            </DialogContentText>

            <form onSubmit={handleSubmit}>
              <Field
                autoFocus
                component={InputField}
                disabled={submitting}
                id={`add-${kind}-name`}
                label={`${capitalize(kind)} Name`}
                name="newGraph"
                required
                type="text"
                validate={composeValidators(
                  required,
                  validServiceName,
                  maxLength(25),
                  checkDuplicate(paperElementNames),
                )}
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
  paperElementNames: PropTypes.arrayOf(PropTypes.string).isRequired,
};

export default ToolboxAddGraphDialog;
