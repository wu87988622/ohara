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
import { toLower } from 'lodash';
import styled, { css } from 'styled-components';
import Button from '@material-ui/core/Button';
import Checkbox from '@material-ui/core/Checkbox';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import CircularProgress from '@material-ui/core/CircularProgress';
import CloseIcon from '@material-ui/icons/Close';
import Typography from '@material-ui/core/Typography';
import IconButton from '@material-ui/core/IconButton';

import DrabblePaper from './DrabblePaper';

const StyledDialogTitle = styled(DialogTitle)(
  props => css`
    cursor: move;

    .close-button {
      position: absolute;
      right: ${props.theme.spacing(1)}px;
      top: ${props.theme.spacing(1)}px;
      color: ${props.theme.palette.grey[500]};
    }
  `,
);

const ConfirmButtonWrapper = styled.div`
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

const DeleteDialog = ({
  cancelText,
  confirmDisabled,
  confirmText,
  content,
  maxWidth,
  open,
  onClose,
  onConfirm,
  isWorking,
  showForceCheckbox,
  testId,
  title,
}) => {
  const [isForceChecked, setIsForceChecked] = useState(false);

  return (
    <Dialog
      maxWidth={maxWidth}
      open={open}
      onClose={onClose}
      PaperComponent={DrabblePaper}
    >
      <div data-testid={testId}>
        <StyledDialogTitle disableTypography>
          <Typography variant="h3">{title}</Typography>
          <IconButton className="close-button" onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </StyledDialogTitle>
        <DialogContent>
          <DialogContentText>{content}</DialogContentText>
          {showForceCheckbox && (
            <div>
              <Checkbox
                checked={isForceChecked}
                onChange={event => setIsForceChecked(event.target.checked)}
                color="primary"
              />{' '}
              {`Force ${toLower(confirmText)}`}
            </div>
          )}
        </DialogContent>
        <DialogActions>
          <Button disabled={isWorking} onClick={onClose}>
            {cancelText}
          </Button>
          <ConfirmButtonWrapper>
            <ConfirmButton
              disabled={isWorking || (confirmDisabled && !isForceChecked)}
              onClick={onConfirm}
              color="primary"
              autoFocus
              data-testid={'confirm-button-' + confirmText}
            >
              {confirmText}
            </ConfirmButton>
            {isWorking && <Progress data-testid="dialog-loader" size={14} />}
          </ConfirmButtonWrapper>
        </DialogActions>
      </div>
    </Dialog>
  );
};

DeleteDialog.propTypes = {
  cancelText: PropTypes.string,
  confirmDisabled: PropTypes.bool,
  confirmText: PropTypes.string,
  content: PropTypes.oneOfType([PropTypes.string, PropTypes.node]).isRequired,
  maxWidth: PropTypes.oneOf(['xs', 'sm', 'md', 'lg', 'xl']),
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
  onConfirm: PropTypes.func,
  isWorking: PropTypes.bool,
  showForceCheckbox: PropTypes.bool,
  testId: PropTypes.string,
  title: PropTypes.string.isRequired,
};

DeleteDialog.defaultProps = {
  cancelText: 'CANCEL',
  confirmDisabled: false,
  confirmText: 'DELETE',
  maxWidth: 'sm',
  onClose: () => {},
  onConfirm: () => {},
  isWorking: false,
  showForceCheckbox: false,
  testId: 'delete-dialog',
};

export default DeleteDialog;
