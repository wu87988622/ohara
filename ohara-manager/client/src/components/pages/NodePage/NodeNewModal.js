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

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import DialogContent from '@material-ui/core/DialogContent';
import { get } from 'lodash';
import { Form, Field, FormSpy } from 'react-final-form';

import * as s from './styles';
import * as MESSAGES from 'constants/messages';
import { InputField } from 'components/common/Mui/Form';
import validate from './validate';
import useSnackbar from 'components/context/Snackbar/useSnackbar';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';
import { Dialog } from 'components/common/Mui/Dialog';

const NodeNewModal = props => {
  const { isOpen, handleClose, handleConfirm } = props;
  const [isValidConnection, setIsValidConnection] = useState(false);
  const { showMessage } = useSnackbar();
  const { getData, postApi } = useApi.usePostApi(URL.NODE_URL);
  const { getData: validateNodeRes, validationApi } = useApi.useValidationApi(
    URL.VALIDATE_NODE_URL,
  );

  const handleModalClose = () => {
    handleClose();
  };

  const onSubmit = async (values, form) => {
    const data = {
      name: values.name,
      password: values.password,
      port: Number(values.port),
      user: values.user,
    };
    await postApi(data);
    const isSuccess = get(getData(), 'data.isSuccess', false);
    if (isSuccess) {
      form.reset();
      showMessage(MESSAGES.NODE_CREATION_SUCCESS);
      handleConfirm();
      handleModalClose();
    }
  };

  const testConnection = async values => {
    const { name: hostname, port, user, password } = values;
    const data = {
      hostname,
      port: Number(port),
      user,
      password,
    };

    await validationApi(data);

    const pass = get(validateNodeRes(), 'data.result[0].pass', false);

    setIsValidConnection(pass);

    if (pass) {
      showMessage(MESSAGES.TEST_SUCCESS);
    }
  };
  return (
    <Form
      onSubmit={onSubmit}
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
            open={isOpen}
            handleClose={handleClose}
            handleConfirm={handleSubmit}
            title="New node"
            confirmBtnText="Add"
            confirmDisabled={
              submitting || pristine || invalid || !isValidConnection
            }
          >
            <div data-testid="new-node-modal">
              <FormSpy
                subscription={{ values: true }}
                onChange={() => {
                  setIsValidConnection(false);
                }}
              />
              <form onSubmit={handleSubmit}>
                <DialogContent>
                  <Field
                    name="name"
                    id="node"
                    label="Node"
                    placeholder="node-00"
                    margin="normal"
                    fullWidth
                    autoFocus
                    component={InputField}
                  />

                  <Field
                    id="port"
                    name="port"
                    label="Port"
                    placeholder="1021"
                    margin="normal"
                    type="number"
                    component={InputField}
                  />

                  <Field
                    id="user"
                    name="user"
                    label="User"
                    placeholder="admin"
                    margin="normal"
                    fullWidth
                    component={InputField}
                  />

                  <Field
                    id="password"
                    name="password"
                    label="Password"
                    type="password"
                    placeholder="password"
                    margin="normal"
                    fullWidth
                    component={InputField}
                  />
                </DialogContent>
                <DialogContent>
                  <s.NewNodeBtn
                    variant="contained"
                    color="primary"
                    text=" Test connection"
                    data-testid="edit-test-connection-button"
                    onClick={() => {
                      testConnection(values);
                    }}
                  />
                </DialogContent>
              </form>
            </div>
          </Dialog>
        );
      }}
    />
  );
};

NodeNewModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  handleClose: PropTypes.func.isRequired,
  handleConfirm: PropTypes.func.isRequired,
};
export default NodeNewModal;
