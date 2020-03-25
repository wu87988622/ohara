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
import { noop } from 'lodash';
import PropTypes from 'prop-types';
import { Form, Field } from 'react-final-form';

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
import * as hooks from 'hooks';

const AddNodeDialog = props => {
  const { isOpen, handleClose, mode } = props;

  const createNode = hooks.useCreateNodeAction();

  const onSubmit = async (values, form) => {
    const { hostname, port, ...rest } = values;

    const params = {
      port: Number(port),
      hostname,
      ...rest,
    };

    createNode(params);
    setTimeout(form.reset);
    handleClose();
  };

  return (
    <Form
      onSubmit={onSubmit}
      initialValues={{}}
      render={({ handleSubmit, form, submitting, pristine, invalid }) => {
        return (
          <Dialog
            open={isOpen}
            handleClose={() => {
              handleClose();
              form.reset();
            }}
            handleConfirm={handleSubmit}
            title="Add node"
            confirmText="ADD"
            confirmDisabled={submitting || pristine || invalid}
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
            </form>
          </Dialog>
        );
      }}
    />
  );
};

AddNodeDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  handleClose: PropTypes.func.isRequired,
  mode: PropTypes.string.isRequired,
};

AddNodeDialog.defaultProps = {
  isOpen: false,
  handleClose: noop,
  mode: MODE.K8S,
};

export default AddNodeDialog;
