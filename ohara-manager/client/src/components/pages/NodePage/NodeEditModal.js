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

import * as nodeApis from 'apis/nodeApis';
import * as validateApis from 'apis/validateApis';
import * as MESSAGES from 'constants/messages';
import * as s from './Styles';
import { InputField } from 'common/FormFields';
import validate from './validate';
import { Modal } from 'common/Modal';
import { Box } from 'common/Layout';
import { FormGroup, Label } from 'common/Form';

class NodeEditModal extends React.Component {
  static propTypes = {
    node: PropTypes.shape({
      name: PropTypes.string,
      port: PropTypes.number,
      user: PropTypes.string,
      password: PropTypes.string,
    }),
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

  onSubmit = async values => {
    const res = await nodeApis.updateNode(values);
    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.NODE_SAVE_SUCCESS);
      this.props.handleConfirm();
      this.handleModalClose();
    }
  };

  testConnection = async values => {
    const { name: hostname, port, user, password } = values;
    this.setState({ isTestBtnWorking: true });
    const res = await validateApis.validateNode({
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
    const { node, isActive } = this.props;
    if (!node) return null;

    const { isValidConnection, isTestBtnWorking } = this.state;
    const { name, port, user, password } = node;

    return (
      <Form
        onSubmit={this.onSubmit}
        initialValues={{ name, port: `${port}`, user, password }}
        validate={validate}
        render={({ handleSubmit, form, submitting, invalid, values }) => {
          return (
            <Modal
              title="Edit ohara node"
              isActive={isActive}
              width="320px"
              handleCancel={this.handleModalClose}
              handleConfirm={handleSubmit}
              confirmBtnText="Save"
              isConfirmDisabled={submitting || invalid || !isValidConnection}
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
                    <Label>Host name</Label>
                    <Field
                      name="name"
                      component={InputField}
                      width="100%"
                      placeholder="node-00"
                      data-testid="name-input"
                      disabled
                    />
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
                      isWorking={isTestBtnWorking}
                      data-testid="test-connection-button"
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

export default NodeEditModal;
