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

import { MODE } from 'const';
import { InputField } from 'components/common/Form';
import { Dialog } from 'components/common/Dialog';
import {
  required,
  minNumber,
  maxNumber,
  composeValidators,
} from 'utils/validate';

const parsePort = (value) => (isNaN(parseInt(value)) ? '' : parseInt(value));

const NodeEditorDialog = ({ isOpen, mode, node, onClose, onConfirm }) => {
  return (
    <Form
      initialValues={{}}
      onSubmit={onConfirm}
      render={({ handleSubmit, form, submitting, pristine, invalid }) => {
        return (
          <Dialog
            confirmDisabled={submitting || pristine || invalid}
            confirmText="SAVE"
            onClose={() => {
              onClose();
              form.reset();
            }}
            onConfirm={handleSubmit}
            open={isOpen}
            title="Edit node"
          >
            <form onSubmit={handleSubmit}>
              <Field
                component={InputField}
                defaultValue={node?.hostname}
                disabled
                helperText="hostname of the node"
                label="Hostname"
                margin="normal"
                name="hostname"
                placeholder="node-01"
                type="text"
              />
              {mode === MODE.DOCKER && (
                <>
                  <Field
                    component={InputField}
                    defaultValue={node?.port}
                    helperText="SSH port of the node"
                    inputProps={{
                      min: 1,
                      max: 65535,
                    }}
                    label="Port"
                    margin="normal"
                    name="port"
                    parse={parsePort}
                    placeholder="22"
                    type="number"
                    validate={composeValidators(
                      required,
                      minNumber(1),
                      maxNumber(65535),
                    )}
                  />
                  <Field
                    component={InputField}
                    defaultValue={node?.user}
                    fullWidth
                    helperText="SSH username"
                    label="User"
                    margin="normal"
                    name="user"
                    placeholder="admin"
                    validate={required}
                  />
                  <Field
                    component={InputField}
                    defaultValue={node?.password}
                    fullWidth
                    helperText="SSH password"
                    label="Password"
                    margin="normal"
                    name="password"
                    placeholder="password"
                    type="password"
                    validate={required}
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

NodeEditorDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.string,
  node: PropTypes.shape({
    hostname: PropTypes.string,
    port: PropTypes.number,
    username: PropTypes.string,
    password: PropTypes.string,
  }),
  onClose: PropTypes.func,
  onConfirm: PropTypes.func.isRequired,
};

NodeEditorDialog.defaultProps = {
  mode: MODE.K8S,
  node: null,
  onClose: () => {},
};

export default NodeEditorDialog;
