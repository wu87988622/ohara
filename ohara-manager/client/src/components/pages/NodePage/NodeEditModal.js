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
import InputField from 'components/common/Mui/Form/InputField';
import DialogTitle from 'components/common/Mui/Dialog/DialogTitle';

import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';

import Button from '@material-ui/core/Button';
import * as nodeApi from 'api/nodeApi';
import * as validateApi from 'api/validateApi';
import validate from './validate';
import * as MESSAGES from 'constants/messages';

class NodeEditModal extends React.Component {
  static propTypes = {
    node: PropTypes.shape({
      name: PropTypes.string,
      port: PropTypes.number,
      user: PropTypes.string,
      password: PropTypes.string,
    }),
    isOpen: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
    handleConfirm: PropTypes.func.isRequired,
  };
  state = {
    isValidConnection: false,
    isTestBtnWorking: false,
  };

  handleModalClose = () => {
    this.props.handleClose();
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
    const { node } = this.props;
    if (!node) return null;
    const { name, port, user, password } = node;
    const { isOpen, handleClose } = this.props;
    return (
      <Form
        onSubmit={this.onSubmit}
        initialValues={{ name, port: `${port}`, user, password }}
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
              open={isOpen}
              onClose={handleClose}
              aria-labelledby="form-dialog-title"
            >
              <div data-testid="edit-node-modal">
                <DialogTitle id="form-dialog-title" onClose={handleClose}>
                  New ohara node
                </DialogTitle>
                <FormSpy
                  subscription={{ values: true }}
                  onChange={() => {
                    this.setState({ isValidConnection: false });
                  }}
                />
                <form onSubmit={handleSubmit}>
                  <DialogContent>
                    <Field
                      disabled
                      name="name"
                      label="Node"
                      placeholder="node-00"
                      margin="normal"
                      fullWidth
                      variant="outlined"
                      component={InputField}
                    />

                    <Field
                      name="port"
                      label="Port"
                      placeholder="1021"
                      margin="normal"
                      type="number"
                      variant="outlined"
                      component={InputField}
                    />

                    <Field
                      name="user"
                      label="User"
                      placeholder="admin"
                      margin="normal"
                      fullWidth
                      variant="outlined"
                      component={InputField}
                    />

                    <Field
                      name="password"
                      label="Password"
                      type="password"
                      placeholder="password"
                      margin="normal"
                      fullWidth
                      variant="outlined"
                      component={InputField}
                    />
                  </DialogContent>
                  <DialogContent>
                    <Button
                      variant="outlined"
                      onClick={e => {
                        e.preventDefault();
                        this.testConnection(values);
                      }}
                      color="primary"
                    >
                      Test connection
                    </Button>
                  </DialogContent>
                  <DialogActions>
                    <Button onClick={handleClose}>Cancel</Button>
                    <Button
                      onClick={handleSubmit}
                      color="primary"
                      disabled={pristine}
                    >
                      Save
                    </Button>
                  </DialogActions>
                </form>
              </div>
            </Dialog>
          );
        }}
      />
    );
  }
}
export default NodeEditModal;
