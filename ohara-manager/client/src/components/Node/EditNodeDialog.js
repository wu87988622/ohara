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
import { get } from 'lodash';
import { Form, Field } from 'react-final-form';

import { InputField } from 'components/common/Form';
import { Dialog } from 'components/common/Dialog';
import {
  required,
  minNumber,
  maxNumber,
  composeValidators,
} from 'utils/validate';
import { useEditNodeDialog } from 'context';
import * as hooks from 'hooks';

const EditNodeDialog = () => {
  const {
    isOpen,
    close: closeEditNodeDialog,
    data: node,
  } = useEditNodeDialog();

  const updateNode = hooks.useUpdateNodeAction();

  const hostname = get(node, 'hostname', '');
  const port = get(node, 'port', '');
  const user = get(node, 'user', '');
  const password = get(node, 'password', '');

  const parse = value => (isNaN(parseInt(value)) ? '' : parseInt(value));

  const onSubmit = async (values, form) => {
    updateNode(values);
    setTimeout(form.reset);
    closeEditNodeDialog();
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
              closeEditNodeDialog();
              form.reset();
            }}
            handleConfirm={handleSubmit}
            title="Edit node"
            confirmText="SAVE"
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
                disabled
                defaultValue={hostname}
              />
              <Field
                name="port"
                label="Port"
                placeholder="22"
                type="number"
                margin="normal"
                helperText="SSH port of the node"
                component={InputField}
                parse={parse}
                defaultValue={port}
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
                defaultValue={user}
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
                defaultValue={password}
                fullWidth
              />
            </form>
          </Dialog>
        );
      }}
    />
  );
};

export default EditNodeDialog;
