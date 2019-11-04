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
import styled from 'styled-components';
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

const Transition = React.forwardRef(function Transition(props, ref) {
  return <Slide direction="up" ref={ref} {...props} />;
});

const StyledDialogTitle = styled(DialogTitle)`
  cursor: move;
  color: ${props => props.theme.palette.primary[500]};

  .brand {
    font-weight: 400;
    display: flex;
    align-items: center;

    svg {
      margin-right: ${props => props.theme.spacing(2)}px;
    }
  }

  .close-button {
    position: absolute;
    right: ${props => props.theme.spacing(1)}px;
    top: ${props => props.theme.spacing(1)}px;
    color: ${props => props.theme.palette.grey[500]};
  }
`;

const StyledDialogContent = styled(DialogContent)`
  margin-bottom: ${props => props.theme.spacing(4)}px;
`;

const StyledDialogActions = styled(DialogActions)`
  justify-content: flex-start;
  padding: ${props => props.theme.spacing(1)}px
    ${props => props.theme.spacing(3)}px ${props => props.theme.spacing(3)}px;

  .quick-start-button {
    margin-right: ${props => props.theme.spacing(2)}px;
  }
`;

const MuiDialog = props => {
  const { isOpen, setIsOpen } = useNewWorkspace();

  const onClose = () => setIsOpen(false);

  // This should be implemented later in #3157
  const onClick = () => {};

  return (
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
          organized their input, output, and streaming applictions with a clean
          and comperhensive GUI
        </Typography>
      </StyledDialogContent>
      <StyledDialogActions>
        <Button
          className="quick-start-button"
          onClick={onClick}
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
  );
};

export default MuiDialog;
