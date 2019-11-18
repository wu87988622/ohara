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

// Import this logo as a React component
// https://create-react-app.dev/docs/adding-images-fonts-and-files/#adding-svgs
import { ReactComponent as Logo } from 'images/logo.svg';

import DrabblePaper from 'components/common/Dialog/DrabblePaper';
import { useNewWorkspace } from 'context/NewWorkspaceContext';
import WorkspaceQuick from '../Workspace/WorkspaceQuick';

const Transition = React.forwardRef(function Transition(props, ref) {
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

    .quick-start-button {
      margin-right: ${theme.spacing(2)}px;
    }
  `,
);

const MuiDialog = () => {
  const { isOpen, setIsOpen } = useNewWorkspace();
  const [quickModeIsOpen, setQuickModeIsOpen] = useState(false);

  const onClose = () => setIsOpen(false);

  // This should be implemented later in #3157
  const onClick = () => {};

  const handleQuickOpen = open => setQuickModeIsOpen(open);

  return (
    <>
      <Dialog
        open={isOpen}
        onClose={onClose}
        maxWidth="sm"
        PaperComponent={DrabblePaper}
        TransitionComponent={Transition}
        fullWidth
        data-testid="dialog-container"
      >
        <StyledDialogTitle>
          <div className="brand">
            <Logo width="38" height="38" />
            <span className="name">Ohara Stream</span>
          </div>
          <IconButton className="close-button" onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </StyledDialogTitle>
        <StyledDialogContent>
          <Typography variant="body2">
            Ohara is a scalable streaming platform that allows users to easily
            organized their input, output, and streaming applications with a
            clean and comprehensive GUI
          </Typography>
        </StyledDialogContent>
        <StyledDialogActions>
          <Button
            className="quick-start-button"
            onClick={() => handleQuickOpen(true)}
            color="primary"
            variant="contained"
            autoFocus
          >
            QUICK START
          </Button>
          or
          <Button onClick={onClick} color="primary">
            CREATE A WORKSPACE IN EXPERT MODE
          </Button>
        </StyledDialogActions>
      </Dialog>
      <WorkspaceQuick open={quickModeIsOpen} handelOpen={handleQuickOpen} />
    </>
  );
};

export default MuiDialog;
