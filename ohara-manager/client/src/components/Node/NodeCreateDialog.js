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
import { FORM_ERROR } from 'final-form';
import Typography from '@material-ui/core/Typography';

import { MODE } from 'const';
import { InputField } from 'components/common/Form';
import { Dialog } from 'components/common/Dialog';
import {
  required,
  minNumber,
  maxLength,
  maxNumber,
  composeValidators,
} from 'utils/validate';

const NodeCreateDialog = ({ isOpen, onClose, onConfirm, mode }) => {
  return (
    <Form
      onSubmit={async (values, form) => {
        try {
          await onConfirm({ ...values, port: Number(values.port) });
          setTimeout(form.reset);
          onClose();
        } catch (error) {
          return { [FORM_ERROR]: error?.title };
        }
      }}
      initialValues={{}}
      render={({ handleSubmit, form, submitting, pristine, submitError }) => {
        return (
          <Dialog
            open={isOpen}
            onClose={() => {
              onClose();
              form.reset();
            }}
            onConfirm={handleSubmit}
            title="Create node"
            confirmText="CREATE"
            confirmDisabled={submitting || pristine}
          >
            <form onSubmit={handleSubmit}>
              <Field
                type="text"
                name="hostname"
                label="Hostname"
                placeholder="node-01"
                margin="normal"
                helperText="hostname of the node"
                component={InputField}
                autoFocus
                required
                validate={composeValidators(required, maxLength(63))}
              />

              {mode === MODE.DOCKER && (
                <>
                  <Field
                    name="port"
                    label="Port"
                    placeholder="22"
                    type="number"
                    margin="normal"
                    helperText="SSH port of the node"
                    component={InputField}
                    validate={composeValidators(
                      required,
                      minNumber(1),
                      maxNumber(65535),
                    )}
                    inputProps={{
                      min: 1,
                      max: 65535,
                    }}
                  />
                  <Field
                    name="user"
                    label="User"
                    placeholder="admin"
                    margin="normal"
                    helperText="SSH username"
                    validate={required}
                    component={InputField}
                    fullWidth
                  />
                  <Field
                    name="password"
                    label="Password"
                    type="password"
                    margin="normal"
                    placeholder="password"
                    helperText="SSH password"
                    validate={required}
                    component={InputField}
                    fullWidth
                  />
                </>
              )}
              {submitError && (
                <Typography align="center" color="error" variant="h5">
                  {submitError}
                </Typography>
              )}
            </form>
          </Dialog>
        );
      }}
    />
  );
};

NodeCreateDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.string,
  onClose: PropTypes.func,
  onConfirm: PropTypes.func.isRequired,
};

NodeCreateDialog.defaultProps = {
  mode: MODE.K8S,
  onClose: () => {},
};

export default NodeCreateDialog;
