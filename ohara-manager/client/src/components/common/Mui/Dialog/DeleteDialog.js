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
import styled from 'styled-components';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import CircularProgress from '@material-ui/core/CircularProgress';

import DrabblePaper from './DrabblePaper';

const StyledDialogTitle = styled(DialogTitle)`
  cursor: move;
`;

const ButtonWrapper = styled.div`
  position: relative;
`;

const ConfirmButton = styled(Button)`
  &[disabled] {
    background-color: ${props => props.theme.palette.action.disabledBackground};
  }
`;

const Progress = styled(CircularProgress)`
  color: green;
  position: absolute;
  top: 50%;
  left: 50%;
  margin-top: -8px;
  margin-left: -8px;
`;

const StyledContentText = styled(DialogContentText)`
  word-break: break-all;
`;

const AlertDialog = props => {
  const {
    title,
    content,
    open,
    handleConfirm,
    handleClose,
    cancelText = 'CANCEL',
    confirmText = 'DELETE',
    isWorking = false,
    testId = 'delete-dialog',
  } = props;

  return (
    <Dialog
      maxWidth="xs"
      open={open}
      onClose={handleClose}
      PaperComponent={DrabblePaper}
    >
      <div data-testid={testId}>
        <StyledDialogTitle>{title}</StyledDialogTitle>
        <DialogContent>
          <StyledContentText>{content}</StyledContentText>
        </DialogContent>
        <DialogActions>
          <Button disabled={isWorking} onClick={handleClose}>
            {cancelText}
          </Button>
          <ButtonWrapper>
            <ConfirmButton
              disabled={isWorking}
              onClick={handleConfirm}
              color="primary"
              autoFocus
              data-testid={'confirm-button-' + confirmText}
            >
              {confirmText}
            </ConfirmButton>
            {isWorking && <Progress data-testid="dialog-loader" size={14} />}
          </ButtonWrapper>
        </DialogActions>
      </div>
    </Dialog>
  );
};

AlertDialog.propTypes = {
  title: PropTypes.string.isRequired,
  content: PropTypes.string.isRequired,
  open: PropTypes.bool.isRequired,
  handleClose: PropTypes.func.isRequired,
  handleConfirm: PropTypes.func.isRequired,
  cancelText: PropTypes.string,
  confirmText: PropTypes.string,
  isWorking: PropTypes.bool,
  testId: PropTypes.string,
};

export default AlertDialog;
