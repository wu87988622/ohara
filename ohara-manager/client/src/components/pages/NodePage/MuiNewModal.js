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
import { get } from 'lodash';
import toastr from 'toastr';
import { Form, Field, FormSpy } from 'react-final-form';
import TextField from '@material-ui/core/TextField';
import { InputField } from 'common/FormFields';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogTitle from '@material-ui/core/DialogTitle';
import Button from '@material-ui/core/Button';
import * as nodeApi from 'api/nodeApi';
import * as validateApi from 'api/validateApi';
import validate from './validate';
import * as MESSAGES from 'constants/messages';

class MuiNewModal extends React.Component {
  state = {
    isValidConnection: false,
    isTestBtnWorking: false,
  };

  onSubmit = async (values, form) => {
    const res = await nodeApi.createNode(values);
    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      form.reset();
      toastr.success(MESSAGES.NODE_CREATION_SUCCESS);
      this.props.handleConfirm();
      this.handleModalClose();
    }
  };

  testConnection = async values => {
    const { name: hostname, port, user, password } = values;
    this.setState({ isTestBtnWorking: true });
    const res = await validateApi.validateNode({
      hostname,
      port,
      user,
      password,
    });

    const pass = get(res, 'data.result[0].pass', false);
    this.setState({ isValidConnection: pass, isTestBtnWorking: false });
    if (pass) {
      toastr.success(MESSAGES.TEST_SUCCESS);
    }
  };
  render() {
    const { newOpen, handleClose, handleConfirm } = this.props;
    const testConnection = this.testConnection;
    return (
      <Form
        onSubmit={this.onSubmit}
        initialValues={{}}
        validate={validate}
        render={({
          handleSubmit,
          form,
          submitting,
          pristine,
          invalid,
          values,
        }) => {
          return (
            <Dialog
              fullWidth={true}
              maxWidth="xs"
              open={newOpen}
              onClose={handleClose}
              aria-labelledby="form-dialog-title"
            >
              <DialogTitle id="form-dialog-title">New ohara node</DialogTitle>
              <FormSpy
                subscription={{ values: true }}
                onChange={() => {
                  this.setState({ isValidConnection: false });
                }}
              />
              <form onSubmit={handleSubmit}>
                <DialogContent>
                  <Field
                    name="name"
                    label="Host Name"
                    component={InputField}
                    placeholder="node-00"
                    data-testid="name"
                    margin="normal"
                    fullWidth
                    maxLength={2}
                  />
                  <TextField
                    id="node"
                    label="Node"
                    placeholder="node-00"
                    margin="normal"
                    fullWidth
                  />

                  <TextField
                    id="port"
                    placeholder="1021"
                    margin="normal"
                    type="number"
                    variant="outlined"
                  />

                  <TextField
                    id="user"
                    placeholder="admin"
                    margin="normal"
                    fullWidth
                    variant="outlined"
                  />

                  <TextField
                    id="password"
                    placeholder="password"
                    margin="normal"
                    fullWidth
                    variant="outlined"
                  />

                  <TextField
                    id="password"
                    placeholder="password"
                    margin="normal"
                    fullWidth
                    variant="outlined"
                  />

                  <Button
                    variant="outlined"
                    onClick={testConnection}
                    color="primary"
                  >
                    Test connection
                  </Button>
                </DialogContent>
                <DialogActions>
                  <Button onClick={handleClose} color="primary">
                    Cancel
                  </Button>
                  <Button onClick={handleSubmit} color="primary">
                    Save
                  </Button>
                </DialogActions>
              </form>
            </Dialog>
          );
        }}
      />
    );
  }
}
export default MuiNewModal;
