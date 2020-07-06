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
import Dialog from '@material-ui/core/Dialog';
import Typography from '@material-ui/core/Typography';
import DialogActions from '@material-ui/core/DialogActions';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';
import Button from '@material-ui/core/Button';
import Slide from '@material-ui/core/Slide';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Close';

import DrabblePaper from 'components/common/Dialog/DrabblePaper';
import { ReactComponent as Logo } from 'images/logo.svg';
import * as hooks from 'hooks';

const Transition = React.forwardRef((props, ref) => {
  return <Slide direction="up" ref={ref} {...props} />;
});

const StyledDialogTitle = styled(DialogTitle)(
  ({ theme }) => css`
    cursor: move;
    color: ${theme.palette.primary[500]};

    .brand {
      font-weight: 400;
      display: flex;
      align-items: center;
      svg {
        margin-right: ${theme.spacing(2)}px;
      }
    }

    .close-button {
      position: absolute;
      right: ${theme.spacing(1)}px;
      top: ${theme.spacing(1)}px;
      color: ${theme.palette.grey[500]};
    }
  `,
);

const StyledDialogContent = styled(DialogContent)(
  ({ theme }) => css`
    margin-bottom: ${theme.spacing(4)}px;
  `,
);

const StyledDialogActions = styled(DialogActions)(
  ({ theme }) => css`
    justify-content: flex-start;
    padding: ${theme.spacing(1)}px ${theme.spacing(3)}px ${theme.spacing(3)}px;

    .quick-mode-button {
      margin-right: ${theme.spacing(2)}px;
    }

    /* Feature is disabled because it's not implemented in 0.9 */
    .expert-mode-button {
      display: none;
    }
  `,
);

const MuiDialog = ({ quickModeText }) => {
  const isIntroDialogOpen = hooks.useIsIntroOpen();
  const closeIntroDialog = hooks.useCloseIntroAction();
  const openWorkspaceDialog = hooks.useOpenCreateWorkspaceDialogAction();

  return (
    <Dialog
      data-testid="intro-dialog"
      fullWidth
      maxWidth="sm"
      onClose={closeIntroDialog}
      open={isIntroDialogOpen}
      PaperComponent={DrabblePaper}
      TransitionComponent={Transition}
    >
      <StyledDialogTitle disableTypography>
        <div className="brand">
          <Logo height="38" width="38" />
          <Typography variant="h3">
            <span className="name">Ohara Stream</span>
          </Typography>
        </div>
        <IconButton
          className="close-button"
          data-testid="close-intro-button"
          onClick={closeIntroDialog}
        >
          <CloseIcon />
        </IconButton>
      </StyledDialogTitle>
      <StyledDialogContent>
        <Typography variant="subtitle1">
          Ohara is a scalable streaming platform that allows users to easily
          organized their input, output, and streaming applications with a clean
          and comprehensive GUI
        </Typography>
      </StyledDialogContent>
      <StyledDialogActions>
        <Button
          autoFocus
          className="quick-mode-button"
          color="primary"
          onClick={openWorkspaceDialog}
          variant="contained"
        >
          {quickModeText}
        </Button>
        <Button className="expert-mode-button" color="primary">
          CREATE A WORKSPACE IN EXPERT MODE
        </Button>
      </StyledDialogActions>
    </Dialog>
  );
};

MuiDialog.propTypes = {
  quickModeText: PropTypes.string.isRequired,
};

export default MuiDialog;
