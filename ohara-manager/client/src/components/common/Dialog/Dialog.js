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
import styled, { css } from 'styled-components';

import Button from '@material-ui/core/Button';
import CircularProgress from '@material-ui/core/CircularProgress';
import DialogActions from '@material-ui/core/DialogActions';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';
import IconButton from '@material-ui/core/IconButton';
import MuiDialog from '@material-ui/core/Dialog';
import Tooltip from '@material-ui/core/Tooltip';
import Typography from '@material-ui/core/Typography';

import CloseIcon from '@material-ui/icons/Close';

import DrabblePaper from './DrabblePaper';

const StyledDialogTitle = styled(DialogTitle)(
  props => css`
    cursor: move;

    h3 {
      width: 95%;
    }

    .close-button {
      position: absolute;
      right: ${props.theme.spacing(1)}px;
      top: ${props.theme.spacing(1)}px;
      color: ${props.theme.palette.grey[500]};
    }
  `,
);

const StyledDialogActions = styled(DialogActions)`
  padding: ${props => props.theme.spacing(1, 3, 3, 3)};
`;

const ConfirmButtonWrapper = styled.div`
  position: relative;
`;

const Progress = styled(CircularProgress)`
  color: green;
  position: absolute;
  top: 50%;
  left: 50%;
  margin-top: -8px;
  margin-left: -8px;
`;

const Dialog = ({
  children,
  confirmDisabled,
  confirmTooltip,
  confirmText,
  cancelText,
  loading,
  maxWidth,
  open,
  onClose,
  onConfirm,
  showActions,
  testId,
  title,
  scroll,
}) => (
  <MuiDialog
    fullWidth
    maxWidth={maxWidth}
    onClose={onClose}
    open={open}
    PaperComponent={DrabblePaper}
    scroll={scroll}
    data-testid={testId}
  >
    <StyledDialogTitle disableTypography>
      <Typography variant="h3">{title}</Typography>
      <IconButton className="close-button" onClick={onClose}>
        <CloseIcon />
      </IconButton>
    </StyledDialogTitle>
    <DialogContent>{children}</DialogContent>
    {showActions && (
      <StyledDialogActions>
        <Button onClick={onClose}>{cancelText}</Button>
        <ConfirmButtonWrapper>
          {confirmDisabled ? (
            <Tooltip title={confirmTooltip}>
              <span>
                <Button
                  disabled={confirmDisabled}
                  color="primary"
                  variant="contained"
                >
                  {confirmText}
                </Button>
              </span>
            </Tooltip>
          ) : (
            <Button onClick={onConfirm} color="primary" variant="contained">
              {confirmText}
            </Button>
          )}

          {loading && <Progress size={14} />}
        </ConfirmButtonWrapper>
      </StyledDialogActions>
    )}
  </MuiDialog>
);

Dialog.propTypes = {
  children: PropTypes.node.isRequired,
  confirmDisabled: PropTypes.bool,
  confirmText: PropTypes.string,
  confirmTooltip: PropTypes.string,
  cancelText: PropTypes.string,
  loading: PropTypes.bool,
  maxWidth: PropTypes.oneOf(['xs', 'sm', 'md', 'lg', 'xl']),
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
  onConfirm: PropTypes.func,
  showActions: PropTypes.bool,
  testId: PropTypes.string,
  title: PropTypes.string.isRequired,
  scroll: PropTypes.string,
};

Dialog.defaultProps = {
  confirmDisabled: false,
  confirmTooltip: '',
  confirmText: 'ADD',
  cancelText: 'CANCEL',
  loading: false,
  maxWidth: 'sm',
  onClose: () => {},
  onConfirm: () => {},
  showActions: true,
  testId: null,
  scroll: 'paper',
};

export default Dialog;
