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
import CircularProgress from '@material-ui/core/CircularProgress';

import DrabblePaper from './DrabblePaper';

const MuiDialog = props => {
  const {
    open,
    handleConfirm,
    handleClose,
    title,
    confirmBtnText = 'ADD',
    cancelBtnText = 'CANCEL',
    children,
    confirmDisabled = false,
    maxWidth = 'xs',
    loading,
    showActions = true,
    testId,
  } = props;
  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth={maxWidth}
      PaperComponent={DrabblePaper}
      fullWidth
      data-testid="dialog-container"
    >
      <div data-testid={testId}>
        <DialogTitle>{title}</DialogTitle>
        {children}

        {showActions && (
          <DialogActions>
            <Button onClick={handleClose} color="primary">
              {cancelBtnText}
            </Button>

            <Button
              onClick={handleConfirm}
              color="primary"
              disabled={confirmDisabled}
              autoFocus
            >
              {!loading && confirmBtnText}
              {loading && (
                <CircularProgress data-testid="dialog-loader" size={24} />
              )}
            </Button>
          </DialogActions>
        )}
      </div>
    </Dialog>
  );
};

MuiDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  title: PropTypes.string.isRequired,
  children: PropTypes.any.isRequired,
  handleClose: PropTypes.func.isRequired,
  confirmBtnText: PropTypes.string,
  cancelBtnText: PropTypes.string,
  maxWidth: PropTypes.string,
  handleConfirm: PropTypes.func,
  confirmDisabled: PropTypes.bool,
  loading: PropTypes.bool,
  testId: PropTypes.string,
  showActions: PropTypes.bool,
};

export default MuiDialog;
