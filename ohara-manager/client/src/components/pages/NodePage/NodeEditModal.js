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
import { get } from 'lodash';
import { Form, Field, FormSpy } from 'react-final-form';
import InputField from 'components/common/Mui/Form/InputField';
import DialogTitle from 'components/common/Mui/Dialog/DialogTitle';

import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';

import Button from '@material-ui/core/Button';
import validate from './validate';
import * as MESSAGES from 'constants/messages';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';
import useSnackbar from 'components/context/Snackbar/useSnackbar';

const NodeEditModal = props => {
  const [isValidConnection, setIsValidConnection] = useState(false);
  const { getData: validateNodeRes, validationApi } = useApi.useValidationApi(
    URL.VALIDATE_NODE_URL,
  );
  const { getData: saveRes, putApi } = useApi.usePutApi(URL.NODE_URL);
  const { showMessage } = useSnackbar();

  const handleModalClose = () => {
    props.handleClose();
  };

  const onSubmit = async (values, form) => {
    const data = {
      name: values.name,
      password: values.password,
      port: Number(values.port),
      user: values.user,
    };

    await putApi(`/${values.name}`, data);
    const isSuccess = get(saveRes(), 'data.isSuccess', false);
    if (isSuccess) {
      form.reset();
      showMessage(MESSAGES.NODE_CREATION_SUCCESS);
      props.handleConfirm();
      handleModalClose();
    }
  };

  const testConnection = async values => {
    const data = {
      hostname: values.name,
      password: values.password,
      port: Number(values.port),
      user: values.user,
    };

    await validationApi(data);

    const pass = get(validateNodeRes(), 'data.result[0].pass', false);

    setIsValidConnection(pass);
    if (pass) {
      showMessage(MESSAGES.TEST_SUCCESS);
    }
  };

  const { node, isOpen, handleClose } = props;
  if (!node) return null;
  const { name, port, user, password } = node;
  return (
    <Form
      onSubmit={onSubmit}
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
                  setIsValidConnection(false);
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
                    data-testid="edit-test-connection-button"
                    variant="outlined"
                    onClick={() => {
                      testConnection(values);
                    }}
                    color="primary"
                  >
                    Test connection
                  </Button>
                </DialogContent>
                <DialogActions>
                  <Button onClick={handleClose} color="primary">
                    Cancel
                  </Button>
                  <Button
                    data-testid="edit-save-button"
                    onClick={handleSubmit}
                    color="primary"
                    disabled={!isValidConnection}
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
};

NodeEditModal.propTypes = {
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
export default NodeEditModal;
