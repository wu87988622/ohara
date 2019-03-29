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
import toastr from 'toastr';
import { Form, Field, FormSpy } from 'react-final-form';
import { get } from 'lodash';

import * as nodeApi from 'api/nodeApi';
import * as validateApi from 'api/validateApi';
import * as s from './Styles';
import * as MESSAGES from 'constants/messages';
import { InputField } from 'common/FormFields';
import validate from './validate';
import { Modal } from 'common/Modal';
import { Box } from 'common/Layout';
import { FormGroup, Label } from 'common/Form';
import DialogContent from '@material-ui/core/DialogContent';

class NodeNewModal extends React.Component {
  static propTypes = {
    isActive: PropTypes.bool.isRequired,
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
    const { isValidConnection, isTestBtnWorking } = this.state;
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
            <Modal
              title="New ohara node"
              isActive={this.props.isActive}
              width="320px"
              handleCancel={() => {
                form.reset();
                this.handleModalClose();
              }}
              handleConfirm={handleSubmit}
              confirmBtnText="Save"
              isConfirmDisabled={
                submitting || pristine || invalid || !isValidConnection
              }
              showActions={true}
            >
              <FormSpy
                subscription={{ values: true }}
                onChange={() => {
                  this.setState({ isValidConnection: false });
                }}
              />
              <form onSubmit={handleSubmit}>
                <Box shadow={false}>
                  <FormGroup data-testid="name">
                    <DialogContent>
                      <Field
                        name="name"
                        label="Host name"
                        component={InputField}
                        width="100%"
                        placeholder="node-00"
                        data-testid="name-input"
                      />
                    </DialogContent>
                  </FormGroup>
                  <FormGroup data-testid="port">
                    <Label>Port</Label>
                    <Field
                      name="port"
                      component={InputField}
                      type="number"
                      min={0}
                      max={65535}
                      width="50%"
                      placeholder="1021"
                      data-testid="port-input"
                    />
                  </FormGroup>
                  <FormGroup data-testid="user">
                    <Label>User name</Label>
                    <Field
                      name="user"
                      component={InputField}
                      width="100%"
                      placeholder="admin"
                      data-testid="user-input"
                    />
                  </FormGroup>
                  <FormGroup data-testid="password">
                    <Label>Password</Label>
                    <Field
                      name="password"
                      component={InputField}
                      type="password"
                      width="100%"
                      placeholder="password"
                      data-testid="password-input"
                    />
                  </FormGroup>
                  <FormGroup data-testid="view-topology">
                    <s.TestConnectionBtn
                      text="Test connection"
                      data-testid="test-connection-button"
                      isWorking={isTestBtnWorking}
                      disabled={isTestBtnWorking}
                      handleClick={e => {
                        e.preventDefault();
                        this.testConnection(values);
                      }}
                    />
                  </FormGroup>
                </Box>
              </form>
            </Modal>
          );
        }}
      />
    );
  }
}

export default NodeNewModal;
