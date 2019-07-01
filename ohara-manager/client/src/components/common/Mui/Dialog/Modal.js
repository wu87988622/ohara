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
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogTitle from '@material-ui/core/DialogTitle';
import Button from '@material-ui/core/Button';

const Modal = props => {
  const {
    handelOpen,
    handelClose,
    title,
    confirmBtnText = 'Add',
    cancelBtnText = 'Cancel',
    handleConfirm,
    children,
    confirmDisabled = false,
  } = props;
  return (
    <Dialog open={handelOpen} onClose={handelClose} maxWidth="xs" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      {children()}
      <DialogActions>
        <Button onClick={handelClose} color="primary">
          {cancelBtnText}
        </Button>
        <Button
          onClick={handleConfirm}
          color="primary"
          autoFocus
          disabled={confirmDisabled}
        >
          {confirmBtnText}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

Modal.propTypes = {
  handelOpen: PropTypes.bool.isRequired,
  handelClose: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  confirmBtnText: PropTypes.string,
  cancelBtnText: PropTypes.string,
  handleConfirm: PropTypes.func.isRequired,
  children: PropTypes.func.isRequired,
  confirmDisabled: PropTypes.bool,
};

export default Modal;
